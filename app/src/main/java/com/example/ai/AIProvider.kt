package com.example.ai

interface AIProvider {
    suspend fun generateText(
        prompt: String,
        systemInstruction: String? = null,
        temperature: Float = 0.4f
    ): Result<String>

    suspend fun analyzeImage(
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg",
        prompt: String,
        systemInstruction: String? = null
    ): Result<String>

    suspend fun analyzeDocument(
        textChunks: List<String>,
        query: String,
        systemInstruction: String? = null
    ): Result<String>

    suspend fun <T> generateStructuredOutput(
        prompt: String,
        schemaDescription: String,
        parser: (String) -> T,
        systemInstruction: String? = null
    ): Result<T>

    suspend fun embed(text: String): Result<List<Float>>
}
