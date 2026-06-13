# Ollama Setup Guide for Farm Manager AI

This guide explains how to install, configure, and verify the local Ollama instance required for the AI-powered Natural Language to SQL features in Farm Manager AI.

## Prerequisites

- Windows 10/11
- Minimum 8GB RAM (16GB recommended for 7B parameter models)
- NVIDIA GPU recommended for faster inference (optional)

## Installation Steps

1. **Download Ollama:**
   - Go to [ollama.com](https://ollama.com/) and download the Windows installer.
   - Run the installer (`OllamaSetup.exe`) and follow the on-screen instructions.

2. **Verify Installation:**
   - Open a terminal (PowerShell or Command Prompt) and check the version:
     ```cmd
     ollama --version
     ```

3. **Pull the Model:**
   - Download the `qwen2.5-coder:7b` model by running:
     ```cmd
     ollama pull qwen2.5-coder:7b
     ```
   - This downloads a ~4.7GB GGUF model optimized for code and SQL generation.

4. **Verify Model is Loaded:**
   - Run the following command to list installed models and confirm `qwen2.5-coder:7b` is present:
     ```cmd
     ollama list
     ```

---

## API Configuration & Integration

The backend is configured to query the local Ollama service at:
- **API Endpoint:** `http://localhost:11434/api/generate`
- **Target Model:** `qwen2.5-coder:7b`

### Service Fallback Behavior
The system includes an automatic fallback mechanism. If the Ollama server is offline or loading the model:
1. The backend detects the HTTP connection timeout/failure.
2. It automatically shifts execution to the regex-based `QueryTemplateService`.
3. The response is flagged with `"mode": "template"`.
4. The frontend displays the **Template Mode** badge instead of **AI Mode**.
