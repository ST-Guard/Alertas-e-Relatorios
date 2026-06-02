package school.sptech.servicos;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import school.sptech.client.ClientS3;
import school.sptech.model.RegistroAlerta;
import school.sptech.model.Severidade;
import school.sptech.repositorio.RegistroRepositorio;
import school.sptech.repositorio.UsuarioRepositorio;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServicoGeradorHistorico {

    private final RegistroRepositorio registroRepositorio;
    private final UsuarioRepositorio usuarioRepositorio;
    private final ClientS3 clientS3;
    private final ObjectMapper objectMapper;

    @Value("${jira.project.key:KAN}")
    private String projetoKey;

    public ServicoGeradorHistorico(RegistroRepositorio registroRepositorio,
                                   UsuarioRepositorio usuarioRepositorio,
                                   ClientS3 clientS3,
                                   ObjectMapper objectMapper) {
        this.registroRepositorio = registroRepositorio;
        this.usuarioRepositorio = usuarioRepositorio;
        this.clientS3 = clientS3;
        this.objectMapper = objectMapper;
    }

    public void gerar() {
        System.out.println("Iniciando geração do historico.json");
        try {

            LocalDateTime corte30d = LocalDateTime.now().minus(30, ChronoUnit.DAYS);
            List<RegistroAlerta> registros = registroRepositorio.findByAbertoEmAfter(corte30d);

            System.out.printf(registros.size() + " registros encontrados nos últimos 30 dias.%n");

            Map<String, Object> historico = new LinkedHashMap<>();

            List<Object[]> listaRaw = registroRepositorio.buscarNomesServidores();

            Map<Integer, String> nomesServidores = listaRaw.stream()
                    .collect(Collectors.toMap(
                            linha -> (Integer) linha[0],
                            linha -> (String) linha[1],
                            (existente, substituto) -> existente
                    ));

            /// historico.put("mttr_por_servidor", agregarMttrPorServidor(registros, nomesServidores));

            Map<String, String> mapeamentoRegioes = Map.of(
                    "DC-SP-01", "SP",
                    "DC-RJ-01", "RJ",
                    "DC-RS-01", "RS"
            );

            Map<String, List<RegistroAlerta>> porDatacenter = registros.stream()
                    .filter(r -> r.getDatacenter() != null && mapeamentoRegioes.containsKey(r.getDatacenter()))
                    .collect(Collectors.groupingBy(RegistroAlerta::getDatacenter));

            Map<String, Object> dadosPorRegiao = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : mapeamentoRegioes.entrySet()) {
                String siglaDc = entry.getKey();
                String nomeRegiao = entry.getValue();

                List<RegistroAlerta> regsDc = porDatacenter.getOrDefault(siglaDc, List.of());

                Map<String, Object> dadosDc = new LinkedHashMap<>();
                dadosDc.put("alertas_por_semana", agregarPorSemana(regsDc));

                dadosDc.put("mttr_por_servidor", agregarMttrPorServidor(regsDc, nomesServidores));

                dadosDc.put("mttr_por_analista", agregarMttrPorAnalista(regsDc));
                dadosDc.put("sla_por_analista", calcularSlaPorAnalista(regsDc));

                Map<String, Object> estruturaInterna = new LinkedHashMap<>();
                estruturaInterna.put(siglaDc, dadosDc);

                dadosPorRegiao.put(nomeRegiao, estruturaInterna);
            }

            historico.put("gerado_em", LocalDateTime.now().toString());
            historico.put("periodo_dias", 30);
            historico.put("regiões", dadosPorRegiao);

            String json = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(historico);

            clientS3.subirJson("client/historico.json", json);
            System.out.println("historico.json gerado e enviado ao S3.");

        } catch (Exception e) {
            System.err.println("Erro ao gerar histórico: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Map<String, Object>> agregarPorSemana(List<RegistroAlerta> registros) {
        Map<String, Map<String, Object>> porSemana = new LinkedHashMap<>();

        for (RegistroAlerta r : registros) {
            if (r.getAbertoEm() == null) continue;

            int semana = r.getAbertoEm().get(WeekFields.ISO.weekOfWeekBasedYear());
            String chave = String.format("S%02d", semana);

            porSemana.computeIfAbsent(chave, k -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("semana", k);
                m.put("inicio", r.getAbertoEm()
                        .with(WeekFields.ISO.dayOfWeek(), 1)
                        .toLocalDate().toString());
                m.put("baixo", 0);
                m.put("medio", 0);
                m.put("critico", 0);
                m.put("total", 0);
                return m;
            });

            Map<String, Object> dados = porSemana.get(chave);
            String sev = r.getSeveridade() != null
                    ? r.getSeveridade().name().toLowerCase() : "baixo";
            dados.put(sev, (int) dados.get(sev) + 1);
            dados.put("total", (int) dados.get("total") + 1);
        }

        return new ArrayList<>(porSemana.values());
    }

    private List<Map<String, Object>> agregarMttrPorServidor(List<RegistroAlerta> registros, Map<Integer, String> nomesServidores) {

        Map<Integer, Map<Severidade, List<Integer>>> porServidor = new HashMap<>();

        for (RegistroAlerta r : registros) {
            if (r.getMttrMinutos() == null || r.getFkServidor() == null) continue;

            porServidor
                    .computeIfAbsent(r.getFkServidor(), k -> new HashMap<>())
                    .computeIfAbsent(r.getSeveridade(), k -> new ArrayList<>())
                    .add(r.getMttrMinutos());
        }

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<Integer, Map<Severidade, List<Integer>>> entry : porServidor.entrySet()) {
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("idServidor", entry.getKey());

            // Agora o 'nomesServidores' está acessível no escopo deste método:
            linha.put("servidor", nomesServidores.getOrDefault(entry.getKey(), "Servidor " + entry.getKey()));

            for (Severidade sev : Severidade.values()) {
                List<Integer> valores = entry.getValue().getOrDefault(sev, List.of());
                double media = valores.stream().mapToInt(i -> i).average().orElse(0.0);
                linha.put(sev.name().toLowerCase(), Math.round(media * 10.0) / 10.0);
            }
            resultado.add(linha);
        }
        return resultado;
    }

    private List<Map<String, Object>> agregarMttrPorAnalista(List<RegistroAlerta> registros) {
        Map<Integer, List<RegistroAlerta>> porAnalista = registros.stream()
                .filter(r -> r.getFkResponsavel() != null && r.getMttrMinutos() != null)
                .collect(Collectors.groupingBy(RegistroAlerta::getFkResponsavel));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<Integer, List<RegistroAlerta>> entry : porAnalista.entrySet()) {
            double mttrMedio = entry.getValue().stream()
                    .mapToInt(RegistroAlerta::getMttrMinutos)
                    .average().orElse(0.0);

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("idAnalista", entry.getKey());
            linha.put("mttrMedio", Math.round(mttrMedio));
            resultado.add(linha);
        }
        return resultado;
    }

    private List<Map<String, Object>> calcularSlaPorAnalista(List<RegistroAlerta> registros) {
        Map<Integer, List<RegistroAlerta>> porAnalista = registros.stream()
                .filter(r -> r.getFkResponsavel() != null && r.getSlaOK() != null)
                .collect(Collectors.groupingBy(RegistroAlerta::getFkResponsavel));

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<Integer, List<RegistroAlerta>> entry : porAnalista.entrySet()) {
            long total = entry.getValue().size();
            long dentroSla = entry.getValue().stream().filter(RegistroAlerta::getSlaOK).count();
            double percentual = total > 0 ? (dentroSla * 100.0 / total) : 0.0;

            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("idAnalista", entry.getKey());
            linha.put("totalChamados", total);
            linha.put("dentroSla", dentroSla);
            linha.put("percentual", Math.round(percentual * 10.0) / 10.0);
            resultado.add(linha);
        }
        return resultado;
    }
}