package com.sptech.school;

import com.sptech.school.config.AppConfig;
import com.sptech.school.config.LoggingConfig;
import com.sptech.school.model.RelatorioFinanceiro;
import com.sptech.school.s3.S3StorageService;
import com.sptech.school.service.PdfService;
import com.sptech.school.service.RelatorioJsonService;
import com.sptech.school.util.Formatador;

import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        LoggingConfig.configurar();

        AppConfig config = AppConfig.fromEnv();
        RelatorioJsonService relatorioJsonService = new RelatorioJsonService();
        PdfService pdfService = new PdfService();

        try (S3StorageService s3StorageService = new S3StorageService(config)) {
            System.out.printf("Baixando JSON s3://%s/%s%n", config.bucket(), config.jsonKey());

            String json = s3StorageService.baixarJson();
            List<RelatorioFinanceiro> relatorios = relatorioJsonService.lerRelatorios(json);

            if (relatorios.isEmpty()) {
                throw new IllegalStateException("Nenhum relatorio encontrado no JSON.");
            }

            for (RelatorioFinanceiro relatorio : relatorios) {
                byte[] pdf = pdfService.gerarPdf(relatorio);
                String destino = relatorios.size() == 1
                        ? config.pdfKey()
                        : chavePdfMensal(config.pdfKey(), relatorio.month());

                s3StorageService.enviarPdf(destino, pdf);
                System.out.printf("PDF enviado para s3://%s/%s (%d bytes)%n", config.bucket(), destino, pdf.length);
            }
        }
    }

    private static String chavePdfMensal(String keyBase, String month) {
        int ponto = keyBase.lastIndexOf('.');
        String sufixo = Formatador.slug(month);
        if (ponto < 0) {
            return keyBase + "-" + sufixo + ".pdf";
        }
        return keyBase.substring(0, ponto) + "-" + sufixo + keyBase.substring(ponto);
    }
}
