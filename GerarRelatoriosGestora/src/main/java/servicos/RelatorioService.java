package servicos;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RelatorioService {

    public String gerarHtmlRelatorio(
            JsonNode json,
            String nomeEmpresa,
            String nomeDatacenter,
            String caminhoTemplate
    ) {
        try {
            String html = Files.readString(Path.of(caminhoTemplate));

            JsonNode empresa = json
                    .get("empresas")
                    .get(nomeEmpresa);

            JsonNode datacenter = empresa
                    .get("datacenters")
                    .get(nomeDatacenter);

            Integer score = datacenter.get("score").asInt();
            String status = datacenter.get("status").asText();
            String regiao = datacenter.get("regiao").asText();

            JsonNode kpiUptime = datacenter.get("kpiUptime");
            JsonNode kpiCrescimentoIncidentes = datacenter.get("kpiCrescimentoAlertas");
            JsonNode kpiServidoresCriticos = datacenter.get("kpiServidoresCriticos");

            Integer totalServidores = kpiServidoresCriticos.get("totalServidores").asInt();
            Integer servidoresCriticos = kpiServidoresCriticos.get("qtdCriticos").asInt();
            Integer servidoresAbaixoUptime = kpiUptime.get("servidoresAbaixoIdeal").asInt();

            String taxaCrescimento = kpiCrescimentoIncidentes.get("valorFormatado").asText();
            String classeStatus = definirClasseStatus(status);
            String linhasZonas = montarLinhasZonas(datacenter);
            String linhasServidoresRisco = montarLinhasServidoresRisco(datacenter);

            LocalDateTime agora = LocalDateTime.now();

            String dataGeracao = agora.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            String horarioGeracao = agora.format(DateTimeFormatter.ofPattern("HH:mm"));

            String resumo = gerarResumo(nomeDatacenter, score, status, servidoresCriticos, totalServidores, servidoresAbaixoUptime);
            html = html.replace("{{NOME_EMPRESA}}", nomeEmpresa);
            html = html.replace("{{NOME_DATACENTER}}", nomeDatacenter);
            html = html.replace("{{REGIAO}}", regiao);
            html = html.replace("{{DATA_GERACAO}}", dataGeracao);
            html = html.replace("{{HORARIO_GERACAO}}", horarioGeracao);

            html = html.replace("{{SCORE_DATACENTER}}", String.valueOf(score));
            html = html.replace("{{STATUS_DATACENTER}}", status);
            html = html.replace("{{CLASSE_STATUS}}", classeStatus);

            html = html.replace("{{TAXA_CRESCIMENTO}}", taxaCrescimento);
            html = html.replace("{{TOTAL_SRV_SCORE_BAIXO}}", String.valueOf(servidoresCriticos));
            html = html.replace("{{TOTAL_SERVIDORES}}", String.valueOf(totalServidores));
            html = html.replace("{{TOTAL_SRV_ABAIXO_UPTIME}}", String.valueOf(servidoresAbaixoUptime));

            html = html.replace("{{LINHAS_ZONAS}}", linhasZonas);
            html = html.replace("{{LINHAS_SERVIDORES_RISCO}}", linhasServidoresRisco);
            String caminhoLogo = Path.of("src/main/resources/assets/logo_smart_azul_preto.png").toAbsolutePath().toUri().toString();

            html = html.replace("{{LOGO_EMPRESA}}", caminhoLogo);
            html = html.replace("{{RESUMO_EXECUTIVO}}", resumo);
            return html;

        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar HTML do relatório", e);
        }
    }

    private String definirClasseStatus(String status) {
        if (status.equalsIgnoreCase("Crítico")) {
            return "critico";
        }

        if (status.equalsIgnoreCase("Atenção")) {
            return "atencao";
        }

        return "saudavel";
    }

    private String montarLinhasZonas(JsonNode datacenter) {
        StringBuilder linhas = new StringBuilder();

        JsonNode zonas = datacenter.get("zonas");

        for (JsonNode zona : zonas) {
            String nomeZona = zona.get("zona").asText();
            Integer score = zona.get("score").asInt();
            String status = zona.get("status").asText();
            Integer qtdServidores = zona.get("qntServidores").asInt();

            linhas.append("""
                    <tr>
                        <td>%s</td>
                        <td>%d</td>
                        <td>%s</td>
                        <td>%d</td>
                    </tr>
                    """.formatted(nomeZona, score, status, qtdServidores));
        }

        return linhas.toString();
    }

private String montarLinhasServidoresRisco(JsonNode datacenter) {
StringBuilder linhas = new StringBuilder();
JsonNode zonas = datacenter.get("zonas");

for (JsonNode zona : zonas) {
    JsonNode servidores = zona.get("servidores");

    for (JsonNode servidor : servidores) {
        String statusServidor = servidor.get("status").asText();

        if (statusServidor.equalsIgnoreCase("Atenção") || statusServidor.equalsIgnoreCase("Crítico")) {
            String nomeServidor = servidor.get("servidor").asText();
            String nomeZona = servidor.get("zona").asText();
            Integer scoreAtual = servidor.get("score").asInt();
            JsonNode projecao = servidor.get("tendenciaDegradacao");
            Integer scoreProjetado = projecao.get("variacaoScore").asInt();
            String risco = projecao.get("nivelRisco").asText();
            String motivo = projecao.get("motivo").asText();

            linhas.append("""
                    <tr>
                        <td>%s</td>
                        <td>%s</td>
                        <td>%d</td>
                        <td>%d</td>
                        <td>%s</td>
                        <td>%s</td>
                    </tr>
                    """.formatted(nomeServidor, nomeZona, scoreAtual, scoreProjetado, risco, motivo));
        }
    }
}

if (linhas.isEmpty()) {
    linhas.append("""
            <tr>
                <td colspan="6">Nenhum servidor em atenção ou crítico no momento.</td>
            </tr>
            """);
}
return linhas.toString();
}

private String gerarResumo(String nomeDatacenter, Integer score, String status, Integer servidoresCriticos, Integer totalServidores, Integer servidoresAbaixoUptime) {
    if (status.equalsIgnoreCase("Crítico")) {
        return "O datacenter " + nomeDatacenter + " encontra-se em estado crítico, com score de saúde " + score +
                "/100. Foram identificados " + servidoresCriticos +
                " servidores críticos entre os " + totalServidores +
                " monitorados. Recomenda-se priorizar a análise dos servidores listados na seção de servidores prioritários.";
    }if (status.equalsIgnoreCase("Atenção")) {
        return "O datacenter " + nomeDatacenter +
                " apresenta sinais de atenção, com score de saúde " + score +
                "/100. É recomendado acompanhar a evolução dos servidores com tendência de degradação e verificar possíveis impactos no uptime operacional.";
    }
    return "O datacenter " + nomeDatacenter + " encontra-se saudável, com score de saúde " + score +
            "/100. No momento, não há sinais críticos relevantes, mas o acompanhamento contínuo deve ser mantido.";
}

}