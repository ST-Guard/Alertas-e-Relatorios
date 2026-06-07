package com.sptech.school.model;

import java.math.BigDecimal;

public record RelatorioFinanceiro(
        String month,
        String generatedAt,
        BigDecimal custo,
        BigDecimal receita,
        BigDecimal margem,
        BigDecimal roi
) {
}
