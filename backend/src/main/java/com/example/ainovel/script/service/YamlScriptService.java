package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.stereotype.Service;

@Service
public class YamlScriptService {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    public String toYaml(ScriptDocument document) {
        try {
            return yamlMapper.writeValueAsString(document);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("剧本 YAML 序列化失败", e);
        }
    }

    public ScriptDocument fromYaml(String yaml) {
        try {
            return yamlMapper.readValue(yaml, ScriptDocument.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("YAML 格式错误：" + e.getOriginalMessage(), e);
        }
    }
}
