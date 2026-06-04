package com.sptech.school.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sptech.school.model.RelatorioFinanceiro;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RelatorioJsonService {
    private static final DateTimeFormatter GENERATED_AT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter JSON_MONTH = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter REPORT_MONTH = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-BR"));

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<RelatorioFinanceiro> lerRelatorios(String json) throws Exception {
        List<JsonNode> nodes = extrairHistoricoMensal(json);
        List<RelatorioFinanceiro> relatorios = new ArrayList<>();

        for (JsonNode node : nodes) {
            relatorios.add(mapearRelatorio(node));
        }

        System.out.printf("Relatorios mensais encontrados: %d%n", relatorios.size());
        return relatorios;
    }

    private List<JsonNode> extrairHistoricoMensal(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode historicoMensal = root.path("HISTORICO_MENSAL");
        int quantidadeMeses = root.path("MODELO").path("N_MESES_HISTORICO").asInt(historicoMensal.size());

        if (!historicoMensal.isArray()) {
            throw new IllegalStateException("Campo HISTORICO_MENSAL nao encontrado ou nao e uma lista no JSON.");
        }

        List<JsonNode> relatorios = new ArrayList<>();
        historicoMensal.forEach(relatorios::add);

        if (quantidadeMeses > 0 && relatorios.size() > quantidadeMeses) {
            return relatorios.subList(relatorios.size() - quantidadeMeses, relatorios.size());
        }

        return relatorios;
    }

    private RelatorioFinanceiro mapearRelatorio(JsonNode node) {
        BigDecimal custo = decimalObrigatorio(node, "custo");
        BigDecimal receita = decimalObrigatorio(node, "receita");
        BigDecimal roi = decimalObrigatorio(node, "roi");
        BigDecimal margem = receita.subtract(custo);

        String month = formatarMes(textoObrigatorio(node, "mes"));
        String gen = LocalDateTime.now().format(GENERATED_AT);

        return new RelatorioFinanceiro(month, gen, custo, receita, margem, roi);
    }

    private BigDecimal decimalObrigatorio(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);

        if (valor.isMissingNode() || valor.isNull()) {
            throw new IllegalStateException("Campo obrigatorio ausente no HISTORICO_MENSAL: " + campo);
        }

        if (valor.isNumber()) {
            return valor.decimalValue();
        }

        if (valor.isTextual() && !valor.asText().isBlank()) {
            return new BigDecimal(valor.asText().trim().replace(",", "."));
        }

        throw new IllegalStateException("Campo " + campo + " precisa ser numerico no HISTORICO_MENSAL.");
    }

    private String textoObrigatorio(JsonNode node, String campo) {
        JsonNode valor = node.path(campo);

        if (valor.isMissingNode() || valor.isNull() || valor.asText().isBlank()) {
            throw new IllegalStateException("Campo obrigatorio ausente no HISTORICO_MENSAL: " + campo);
        }

        return valor.asText();
    }

    private String formatarMes(String mes) {
        YearMonth yearMonth = YearMonth.parse(mes, JSON_MONTH);
        String mesFormatado = yearMonth.format(REPORT_MONTH);
        return mesFormatado.substring(0, 1).toUpperCase(Locale.forLanguageTag("pt-BR")) + mesFormatado.substring(1);
    }
}
