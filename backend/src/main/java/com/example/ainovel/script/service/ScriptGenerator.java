package com.example.ainovel.script.service;

import com.example.ainovel.script.model.ScriptDocument;

public interface ScriptGenerator {

    ScriptDocument generate(String novelText);
}
