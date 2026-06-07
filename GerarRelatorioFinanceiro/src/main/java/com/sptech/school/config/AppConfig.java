package com.sptech.school.config;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

public record AppConfig(
        String bucket,
        String jsonKey,
        String pdfKey,
        Region region,
        String awsAccessKeyId,
        String awsSecretAccessKey,
        String awsSessionToken
) {
    private static final Dotenv DOTENV = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    public static AppConfig fromEnv() {
        return new AppConfig(
                getEnv("S3_BUCKET", "smartdatabucket2"),
                getEnv("S3_JSON_KEY", "client/dashFinanceira.json"),
                getEnv("S3_PDF_KEY", "relatorios/relatorio-financeiro.pdf"),
                Region.of(getEnv("AWS_REGION", "us-east-1")),
                getEnv("aws_access_key_id", "aws_access_key_id", ""),
                getEnv("aws_secret_access_key", "aws_secret_access_key", ""),
                getEnv("aws_session_token", "aws_session_token", "")
        );
    }

    public AwsCredentialsProvider credentialsProvider() {
        if (awsAccessKeyId.isBlank() || awsSecretAccessKey.isBlank()) {
            return DefaultCredentialsProvider.create();
        }

        if (awsSessionToken.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(awsAccessKeyId, awsSecretAccessKey)
            );
        }

        return StaticCredentialsProvider.create(
                AwsSessionCredentials.create(awsAccessKeyId, awsSecretAccessKey, awsSessionToken)
        );
    }

    private static String getEnv(String nome, String padrao) {
        String valor = DOTENV.get(nome);
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(nome);
        }
        return limparValor(valor == null || valor.isBlank() ? padrao : valor);
    }

    private static String getEnv(String nome, String alias, String padrao) {
        String valor = DOTENV.get(nome);
        if (valor == null || valor.isBlank()) {
            valor = DOTENV.get(alias);
        }
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(nome);
        }
        if (valor == null || valor.isBlank()) {
            valor = System.getenv(alias);
        }
        return limparValor(valor == null || valor.isBlank() ? padrao : valor);
    }

    private static String limparValor(String valor) {
        String limpo = valor.trim();
        if ((limpo.startsWith("\"") && limpo.endsWith("\"")) || (limpo.startsWith("'") && limpo.endsWith("'"))) {
            return limpo.substring(1, limpo.length() - 1).trim();
        }
        return limpo;
    }
}
