package servicos;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class PdfService {

public void gerarPdf(String html, String caminhoPdf) {
    try {
        File arquivoPdf = new File(caminhoPdf);

        File pastaPai = arquivoPdf.getParentFile();

        if (pastaPai != null) {
            pastaPai.mkdirs();
        }

        try (OutputStream os = new FileOutputStream(arquivoPdf)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();

            builder.useFastMode();

            builder.withHtmlContent(html, new File(".").toURI().toString());

            builder.toStream(os);

            builder.run();
        }

    } catch (Exception e) {
        throw new RuntimeException("Erro ao gerar PDF", e);
    }
}
}