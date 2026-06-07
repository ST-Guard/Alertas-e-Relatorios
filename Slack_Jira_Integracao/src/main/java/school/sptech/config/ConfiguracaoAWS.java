package school.sptech.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class ConfiguracaoAWS {

    @Value("${AWS_ACCESS_KEY_ID}")
    private String keyId;

    @Value("${AWS_SECRET_ACCESS_KEY}")
    private String secretKey;

    @Value("${AWS_SESSION_TOKEN}")
    private String sessionToken;

    @Value("${AWS_REGION}")
    private String regiao;

    @Bean
    public S3Client s3Client() {
        AwsSessionCredentials credenciais = AwsSessionCredentials
                .create(keyId, secretKey, sessionToken);

        return S3Client.builder()
                .region(Region.of(regiao))
                .credentialsProvider(StaticCredentialsProvider.create(credenciais))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}