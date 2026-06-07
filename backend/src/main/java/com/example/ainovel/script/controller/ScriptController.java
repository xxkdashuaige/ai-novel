package com.example.ainovel.script.controller;

import com.example.ainovel.script.service.ConversionResponse;
import com.example.ainovel.script.service.AiEditingService;
import com.example.ainovel.script.service.AiStatusResponse;
import com.example.ainovel.script.service.AiStatusService;
import com.example.ainovel.script.service.ConsistencyRepairService;
import com.example.ainovel.script.service.EditResponse;
import com.example.ainovel.script.service.ScriptConversionService;
import com.example.ainovel.script.service.ScriptHistoryItem;
import com.example.ainovel.script.service.ScriptHistoryService;
import com.example.ainovel.script.service.ScriptValidationService;
import com.example.ainovel.script.service.SceneEditResponse;
import com.example.ainovel.script.service.ValidationResult;
import com.example.ainovel.script.model.ScriptDocument;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;
import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ScriptController {

    private final ScriptConversionService conversionService;
    private final ScriptValidationService validationService;
    private final AiEditingService editingService;
    private final ConsistencyRepairService repairService;
    private final ScriptHistoryService historyService;
    private final AiStatusService aiStatusService;

    public ScriptController(
            ScriptConversionService conversionService,
            ScriptValidationService validationService,
            AiEditingService editingService,
            ConsistencyRepairService repairService,
            ScriptHistoryService historyService,
            AiStatusService aiStatusService
    ) {
        this.conversionService = conversionService;
        this.validationService = validationService;
        this.editingService = editingService;
        this.repairService = repairService;
        this.historyService = historyService;
        this.aiStatusService = aiStatusService;
    }

    @PostMapping("/scripts/convert")
    public ConversionResponse convert(@Valid @RequestBody ConvertRequest request) {
        return saveAndReturn(conversionService.convert(request.novelText()));
    }

    @PostMapping("/scripts/render")
    public ConversionResponse render(@Valid @RequestBody ScriptDocument document) {
        return saveAndReturn(conversionService.render(document));
    }

    @PostMapping("/scripts/repair")
    public ConversionResponse repair(@Valid @RequestBody ScriptDocument document) {
        return saveAndReturn(conversionService.render(repairService.repair(document)));
    }

    @PostMapping("/scripts/edit")
    public EditResponse edit(@Valid @RequestBody EditRequest request) {
        return editingService.edit(request);
    }

    @PostMapping("/scripts/edit-scene")
    public SceneEditResponse editScene(@Valid @RequestBody SceneEditRequest request) {
        return editingService.editScene(request);
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

    @GetMapping("/ai/status")
    public AiStatusResponse aiStatus() {
        return aiStatusService.status();
    }

    @GetMapping("/scripts/history")
    public List<ScriptHistoryItem> history() {
        return historyService.list();
    }

    @DeleteMapping("/scripts/history/{id}")
    public ResponseEntity<Void> deleteHistory(@PathVariable String id) {
        historyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/scripts/history")
    public ResponseEntity<Void> clearHistory() {
        historyService.clear();
        return ResponseEntity.noContent().build();
    }

    private ConversionResponse saveAndReturn(ConversionResponse response) {
        historyService.save(response);
        return response;
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
