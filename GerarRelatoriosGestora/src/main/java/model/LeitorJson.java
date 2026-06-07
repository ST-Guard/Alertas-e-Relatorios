package model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

public class LeitorJson {
private final ObjectMapper mapper = new ObjectMapper();

public JsonNode lerJson(String caminhoArquivo) {
    try {
        return mapper.readTree(new File(caminhoArquivo));
    } catch (Exception e) {
        throw new RuntimeException("Erro ao ler JSON local", e);
    }
}

public JsonNode lerJsonDeTexto(String jsonTexto) {
    try {
        return mapper.readTree(jsonTexto);
    } catch (Exception e) {
        throw new RuntimeException("Erro ao ler JSON a partir do texto baixado do S3", e);
    }
}
}