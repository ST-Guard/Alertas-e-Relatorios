package com.relatorio;
import com.fasterxml.jackson.databind.JsonNode;
import model.LeitorJson;
import servicos.PdfService;
import servicos.RelatorioService;
import servicos.S3Service;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    private static final String BUCKET = System.getenv("S3_BUCKET");
    private static final String CHAVE_JSON_S3 = System.getenv("S3_JSON_KEY");
    private static final String CAMINHO_TEMPLATE = "src/main/resources/templates/modeloRelatorioGestorOP.html";

    public static void main(String[] args) {

        ScheduledExecutorService agendador = Executors.newSingleThreadScheduledExecutor();

        Runnable tarefaGerarRelatorios = () -> {
            try {
                System.out.println("---------------------------------------------------");

                System.out.println("Iniciando rotina automática de relatórios");
                System.out.println("Horário: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
                System.out.println("---------------------------------------------------");


                executarGeracaoRelatorios();

                System.out.println("---------------------------------------------------");
                System.out.println("Rotina finalizada com sucesso.");
                System.out.println("Próxima execução em 30 minutos.");
                System.out.println("---------------------------------------------------");

            } catch (Exception e) {
                System.out.println("Erro geral na rotina automática de relatórios.");
                e.printStackTrace();
            }
        };

        agendador.scheduleAtFixedRate(tarefaGerarRelatorios, 0, 30, TimeUnit.MINUTES);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando agendador de relatórios...");
            agendador.shutdown();
        }));
    }

private static void executarGeracaoRelatorios() {

    S3Service s3Service = new S3Service();
    LeitorJson leitorJson = new LeitorJson();
    RelatorioService relatorioService = new RelatorioService();
    PdfService pdfService = new PdfService();

    System.out.println("Baixando JSON do S3...");

    String jsonTexto = s3Service.baixarArquivoComoTexto(BUCKET, CHAVE_JSON_S3);

    System.out.println("JSON baixado com sucesso.");

    JsonNode json = leitorJson.lerJsonDeTexto(jsonTexto);

    JsonNode empresas = json.get("empresas");

    if (empresas == null || empresas.isMissingNode()) {
        throw new RuntimeException("O JSON não possui o campo 'empresas'.");
    }

    Iterator<Map.Entry<String, JsonNode>> empresasIterator = empresas.fields();

    while (empresasIterator.hasNext()) {

        Map.Entry<String, JsonNode> empresaEntry = empresasIterator.next();

        String nomeEmpresa = empresaEntry.getKey();
        JsonNode empresa = empresaEntry.getValue();

        JsonNode datacenters = empresa.get("datacenters");

        if (datacenters == null || datacenters.isMissingNode()) {
            System.out.println("Empresa " + nomeEmpresa + " não possui datacenters. Pulando...");
            continue;
        }

        Iterator<Map.Entry<String, JsonNode>> datacentersIterator = datacenters.fields();

        while (datacentersIterator.hasNext()) {
            Map.Entry<String, JsonNode> datacenterEntry = datacentersIterator.next();
            String nomeDatacenter = datacenterEntry.getKey();
            gerarRelatorioDatacenter(BUCKET, nomeEmpresa, nomeDatacenter, json, CAMINHO_TEMPLATE, relatorioService, pdfService, s3Service);
        }
    }

    System.out.println("Processo de geração finalizado.");
}

private static void gerarRelatorioDatacenter(
        String bucket,
        String nomeEmpresa,
        String nomeDatacenter,
        JsonNode json,
        String caminhoTemplate,
        RelatorioService relatorioService,
        PdfService pdfService,
        S3Service s3Service
) {
    try {
        System.out.println("--------------------------------------");
        System.out.println("Gerando relatório");
        System.out.println("Empresa: " + nomeEmpresa);
        System.out.println("Datacenter: " + nomeDatacenter);

        String dataArquivo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));

        String nomeArquivoSeguroEmpresa = formatarNomeArquivo(nomeEmpresa);
        String nomeArquivoSeguroDatacenter = formatarNomeArquivo(nomeDatacenter);

        String nomePdf = "relatorio-" + nomeArquivoSeguroDatacenter + "-" + dataArquivo + ".pdf";
        String caminhoHtmlDebug = "output/html/" + nomeArquivoSeguroEmpresa + "/" + nomeArquivoSeguroDatacenter + "/relatorio-preenchido.html";
        String caminhoPdfLocal = "output/pdf/" + nomeArquivoSeguroEmpresa + "/" + nomeArquivoSeguroDatacenter + "/" + nomePdf;

        String chavePdfS3 = "relatorios/gestora/" + nomeArquivoSeguroEmpresa + "/" + nomeArquivoSeguroDatacenter + "/" + nomePdf;

        String htmlPreenchido = relatorioService.gerarHtmlRelatorio(json, nomeEmpresa, nomeDatacenter, caminhoTemplate);

        Path caminhoHtml = Path.of(caminhoHtmlDebug);

        if (caminhoHtml.getParent() != null) {
            Files.createDirectories(caminhoHtml.getParent());
        }

        Files.writeString(caminhoHtml, htmlPreenchido);

        System.out.println("HTML gerado em: " + caminhoHtmlDebug);

        pdfService.gerarPdf(htmlPreenchido, caminhoPdfLocal);

        System.out.println("PDF gerado localmente em: " + caminhoPdfLocal);

        s3Service.enviarArquivo(bucket, chavePdfS3, caminhoPdfLocal, "application/pdf");

        System.out.println("PDF enviado para o S3.");
        System.out.println("Caminho no S3: s3://" + bucket + "/" + chavePdfS3);

    } catch (Exception e) {
        System.out.println("Erro ao gerar relatório para o datacenter " + nomeDatacenter);
        e.printStackTrace();
    }
}

private static String formatarNomeArquivo(String texto) {
    return texto.replace(" ", "-").replace("/", "-").replace("\\", "-").replace(":", "-");
}
}