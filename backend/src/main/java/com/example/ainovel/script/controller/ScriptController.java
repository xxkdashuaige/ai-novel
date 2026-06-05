package com.example.ainovel.script.controller;

import com.example.ainovel.script.service.ConversionResponse;
import com.example.ainovel.script.service.ScriptConversionService;
import com.example.ainovel.script.service.ScriptValidationService;
import com.example.ainovel.script.service.ValidationResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScriptController {

    private final ScriptConversionService conversionService;
    private final ScriptValidationService validationService;

    public ScriptController(ScriptConversionService conversionService, ScriptValidationService validationService) {
        this.conversionService = conversionService;
        this.validationService = validationService;
    }

    @PostMapping("/scripts/convert")
    public ConversionResponse convert(@Valid @RequestBody ConvertRequest request) {
        return conversionService.convert(request.novelText());
    }

    @PostMapping("/scripts/validate")
    public ValidationResult validate(@Valid @RequestBody ValidateRequest request) {
        return validationService.validateYaml(request.yaml());
    }

    @GetMapping("/schema")
    public Map<String, Object> schema() {
        return Map.of(
                "title", "剧本 YAML Schema",
                "required", new String[]{"title", "chapters", "metadata"},
                "chapterRule", "chapters 至少 3 章，每章至少 1 个 scene",
                "beatTypes", new String[]{"dialogue", "action", "narration", "transition"}
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> Objects.requireNonNullElse(error.getDefaultMessage(), error.getField() + " 参数不合法"))
                .findFirst()
                .orElse("请求参数不合法");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadableBody() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", "请求体不是合法 JSON"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleServerError(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "服务处理失败：" + ex.getMessage()));
    }
}
