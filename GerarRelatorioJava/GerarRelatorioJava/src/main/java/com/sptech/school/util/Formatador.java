package com.sptech.school.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;

public class Formatador {
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private Formatador() {
    }

    public static BigDecimal calcularPercentual(BigDecimal parte, BigDecimal total) {
        if (total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }

    public static String formatarMoeda(BigDecimal valor) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(PT_BR);
        return formatter.format(valor);
    }

    public static String formatarPercentual(BigDecimal valor) {
        if (valor.abs().compareTo(BigDecimal.ZERO) > 0 && valor.abs().compareTo(BigDecimal.ONE) <= 0) {
            valor = valor.multiply(BigDecimal.valueOf(100));
        }
        return valor.setScale(0, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    public static String escaparHtml(String valor) {
        return valor
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static String slug(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return semAcento.toLowerCase(PT_BR)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
