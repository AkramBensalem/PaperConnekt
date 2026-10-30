package com.akram.bensalem.paper.connekt.core.aiassistant

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.mistralai.MistralAILLMClient
import ai.koog.prompt.executor.clients.mistralai.MistralAIModels
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import com.paperconnekt.knowledge.EmbeddingProvider
import com.paperconnekt.knowledge.contentHash
import com.paperconnekt.secrets.SecretKey
import com.paperconnekt.secrets.SecretStore
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/**
 * Mistral `mistral-embed` through Koog. Mistral embeds many texts per request, so texts are grouped into requests of
 * at most [maxCharsPerRequest] characters (well under the model's token limit).
 */
class MistralEmbeddingProvider(apiKey: String, private val maxCharsPerRequest: Int = 24_000) : EmbeddingProvider {
    private val client = MistralAILLMClient(apiKey = apiKey, httpClientFactory = KtorKoogHttpClient.Factory(HttpClient()))
    override val modelId = "mistral/${MistralAIModels.Embeddings.MistralEmbed.id}"

    override suspend fun embed(texts: List<String>): List<FloatArray> {
        val requests = mutableListOf<MutableList<String>>()
        var chars = 0
        texts.forEach { text ->
            if (requests.isEmpty() || chars + text.length > maxCharsPerRequest) {
                requests += mutableListOf<String>()
                chars = 0
            }
            requests.last() += text
            chars += text.length
        }
        return requests.flatMap { request ->
            client.embed(request, MistralAIModels.Embeddings.MistralEmbed).map { it.toFloatArray() }
        }
    }
}

/** OpenAI `text-embedding-3-small` through Koog. Koog's OpenAI client embeds one text per request, so a few run in parallel. */
class OpenAiEmbeddingProvider(apiKey: String, parallelRequests: Int = 4) : EmbeddingProvider {
    private val client = OpenAILLMClient(apiKey = apiKey, httpClientFactory = KtorKoogHttpClient.Factory(HttpClient()))
    private val permits = Semaphore(parallelRequests)
    override val modelId = "openai/${OpenAIModels.Embeddings.TextEmbedding3Small.id}"

    override suspend fun embed(texts: List<String>): List<FloatArray> = coroutineScope {
        texts.map { text ->
            async {
                permits.withPermit { client.embed(text, OpenAIModels.Embeddings.TextEmbedding3Small).toFloatArray() }
            }
        }.awaitAll()
    }
}

/**
 * Resolves the embedding model from the saved provider keys, preferring Mistral (batched requests, and the paper's
 * text already went to Mistral for OCR) over OpenAI. Returns null without a key. The provider is reused until a key
 * changes, so its HTTP client is not recreated for every search.
 */
class EmbeddingProviders(private val secretStore: SecretStore?) {
    private val lock = Mutex()
    private var cached: Pair<Long, EmbeddingProvider>? = null

    suspend fun current(): EmbeddingProvider? {
        val mistral = read(SecretKey.MISTRAL)
        val openAi = read(SecretKey.OPENAI)
        val key = when {
            mistral.isNotEmpty() -> contentHash("mistral:$mistral")
            openAi.isNotEmpty() -> contentHash("openai:$openAi")
            else -> return null
        }
        return lock.withLock {
            cached?.takeIf { it.first == key }?.second
                ?: (if (mistral.isNotEmpty()) MistralEmbeddingProvider(mistral) else OpenAiEmbeddingProvider(openAi))
                    .also { cached = key to it }
        }
    }

    private suspend fun read(key: SecretKey): String = try {
        secretStore?.read(key)?.trim().orEmpty()
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        ""
    }
}

private fun List<Double>.toFloatArray(): FloatArray = FloatArray(size) { this[it].toFloat() }
