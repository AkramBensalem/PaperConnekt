package com.akram.bensalem.paper.connekt.shared.zotero

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

/** Looks up citation counts for DOIs. */
fun interface CitationCountClient {
    /** Returns citation counts keyed by [normalizeDoi] form; DOIs that are not found are absent. */
    suspend fun citationCounts(dois: Collection<String>): Map<String, Int>
}

/** Lower-cased bare DOI (`10.x/y`) without `https://doi.org/` or `doi:` prefixes, or null when it is not a DOI. */
fun normalizeDoi(value: String): String? {
    val doi = value.trim()
        .replace(Regex("""^(?:https?://)?(?:dx\.)?doi\.org/""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""^doi:\s*""", RegexOption.IGNORE_CASE), "")
        .trim().lowercase()
    return doi.takeIf { it.startsWith("10.") && '/' in it && ',' !in it && '|' !in it }
}

/** Batched citation-count lookup through the OpenAlex `works` API (50 DOIs per request). */
class OpenAlexCitationClient(
    private val baseUrl: String = "https://api.openalex.org",
    private val mailto: String? = null
) : CitationCountClient {

    override suspend fun citationCounts(dois: Collection<String>): Map<String, Int> {
        val normalized = dois.mapNotNull(::normalizeDoi).distinct()
        if (normalized.isEmpty()) return emptyMap()
        val counts = mutableMapOf<String, Int>()
        HttpClient {
            expectSuccess = false
            install(HttpTimeout) { requestTimeoutMillis = 30_000; connectTimeoutMillis = 10_000 }
            defaultRequest {
                header(HttpHeaders.Accept, ContentType.Application.Json)
                header(HttpHeaders.UserAgent, "PaperConnekt/1.0")
            }
        }.use { http ->
            normalized.chunked(BatchSize).forEach { batch ->
                val response = http.get("$baseUrl/works") {
                    parameter("filter", "doi:" + batch.joinToString("|"))
                    parameter("select", "doi,cited_by_count")
                    parameter("per-page", BatchSize)
                    mailto?.let { parameter("mailto", it) }
                }
                if (!response.status.isSuccess()) {
                    throw IllegalStateException("OpenAlex returned HTTP ${response.status.value}.")
                }
                Json.parseToJsonElement(response.bodyAsText()).jsonObject["results"]?.jsonArray?.forEach { work ->
                    val obj = work.jsonObject
                    val doi = obj["doi"]?.jsonPrimitive?.contentOrNull?.let(::normalizeDoi) ?: return@forEach
                    val cited = obj["cited_by_count"]?.jsonPrimitive?.intOrNull ?: return@forEach
                    counts[doi] = maxOf(cited, counts[doi] ?: 0)
                }
            }
        }
        return counts
    }

    private companion object {
        const val BatchSize = 50
    }
}
