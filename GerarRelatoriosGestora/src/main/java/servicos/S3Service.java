package servicos;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class S3Service {
private final S3Client s3Client;

public S3Service() {
    this.s3Client = S3Client.builder().region(Region.US_EAST_1).credentialsProvider(DefaultCredentialsProvider.create()).build();
}

public String baixarArquivoComoTexto(String bucket, String chaveS3) {
    try {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(chaveS3).build();
        ResponseInputStream<GetObjectResponse> objetoS3 = s3Client.getObject(request);

        return new String(objetoS3.readAllBytes(), StandardCharsets.UTF_8);

    } catch (Exception e) {
        throw new RuntimeException("Erro ao baixar arquivo do S3: " + chaveS3, e);
    }
}

public void baixarArquivoParaLocal(String bucket, String chaveS3, String caminhoLocal) {
    try {
        GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(chaveS3).build();

        Path destino = Path.of(caminhoLocal);

        if (destino.getParent() != null) {
            Files.createDirectories(destino.getParent());
        }

        s3Client.getObject(request, destino);

    } catch (Exception e) {
        throw new RuntimeException("Erro ao baixar arquivo do S3 para local: " + chaveS3, e);
    }
}

public void enviarArquivo(String bucket, String chaveS3, String caminhoArquivoLocal, String contentType) {
    try {
        PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(chaveS3).contentType(contentType).build();

        s3Client.putObject(request, RequestBody.fromFile(Path.of(caminhoArquivoLocal)));

    } catch (Exception e) {
        throw new RuntimeException("Erro ao enviar arquivo para o S3: " + chaveS3, e);
    }
}
}