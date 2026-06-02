package school.sptech.client;

import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Bean;
import school.sptech.model.Alerta; ///
import school.sptech.model.RespostaAlertaGestora; ///
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value; ///
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;

import java.io.InputStream; ///
import java.time.LocalDateTime; ///
import java.util.ArrayList; ///
import java.util.List; ///
import java.util.Map; ///

@Component
public class ClientS3 {
    private final S3Client s3;
    private final ObjectMapper objectMapper;

    @Value("${aws.bucket-nome}")
    private String nomeBucket;

    private static final String CHAVE_ALERTA = "client/alertas_gestora.json";

    public ClientS3(S3Client s3, ObjectMapper objectMapper) {
        this.s3 = s3;
        this.objectMapper = objectMapper;
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public List<Alerta> baixarAlertasPendentes() {
        List<Alerta> todosAlertas = new ArrayList<>();

        try {

            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(nomeBucket)
                    .key(CHAVE_ALERTA)
                    .build();

            InputStream inputStream = s3.getObject(request);


            Map<String, Map<String, RespostaAlertaGestora.DadosDatacenter>> resposta =
                    objectMapper.readValue(inputStream,
                            new TypeReference<Map<String, Map<String, RespostaAlertaGestora.DadosDatacenter>>>() {
                            }
                    );

            for (String empresa : resposta.keySet()) {
                for (String datacenter : resposta.get(empresa).keySet()) {
                    RespostaAlertaGestora.DadosDatacenter dados =
                            resposta.get(empresa).get(datacenter);

                    for (Alerta alerta : dados.getAlertasAtivos()) {
                        alerta.setDatacenter(datacenter);
                        todosAlertas.add(alerta);
                    }
                }
            }

            System.out.println("Foram baixados um total de " + todosAlertas.size() + " do client S3");
        } catch (Exception e) {
            System.out.println("Error ao baixar os alertas: " + e.getMessage());
        }

        return todosAlertas.stream()
                .filter(a -> a.getIssueKey() == null || a.getIssueKey().isEmpty())
                .toList();
    }

    public void atualizarIssuesKeys(String servidor, String componente, LocalDateTime timestamp, String issueKey) {
        System.out.println("Atualizar issueKey " + issueKey + " para alerta de " + servidor);
    }

    public void subirJson(String chave, String conteudo) {
        try {
            byte[] bytes = conteudo.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            software.amazon.awssdk.services.s3.model.PutObjectRequest request =
                    software.amazon.awssdk.services.s3.model.PutObjectRequest.builder()
                            .bucket(nomeBucket)
                            .key(chave)
                            .contentType("application/json")
                            .build();

            s3.putObject(request,
                    software.amazon.awssdk.core.sync.RequestBody.fromBytes(bytes));

            System.out.println("Upload concluído: " + chave);
        } catch (Exception e) {
            System.err.println("Erro no upload: " + e.getMessage());
        }
    }
}

