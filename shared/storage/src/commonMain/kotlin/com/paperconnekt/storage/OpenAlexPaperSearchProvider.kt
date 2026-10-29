package com.paperconnekt.storage

import com.paperconnekt.domain.model.Paper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class OpenAlexPaperSearchProvider(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : PaperSearchProvider {
    override suspend fun search(query: String): List<Paper> {
        return search(query, page = 0)
    }

    suspend fun search(query: String, page: Int, perPage: Int = 10): List<Paper> {
        val response = client.get("https://api.openalex.org/works") {
            parameter("search", query)
            parameter("page", page.coerceAtLeast(0) + 1)
            parameter("per-page", perPage.coerceIn(1, 100))
            parameter("select", "id,title,publication_year,authorships,doi,open_access,cited_by_count,referenced_works")
        }
        val payload = json.decodeFromString<OpenAlexResponse>(response.bodyAsText())
        return payload.results.mapNotNull { it.toPaper() }
    }
}

@Serializable
private data class OpenAlexResponse(
    val results: List<OpenAlexWork> = emptyList(),
)

@Serializable
private data class OpenAlexWork(
    val id: String,
    val title: String? = null,
    @SerialName("publication_year")
    val publicationYear: Int? = null,
    val authorships: List<OpenAlexAuthorship> = emptyList(),
    val doi: String? = null,
    @SerialName("cited_by_count")
    val citedByCount: Int? = null,
    @SerialName("referenced_works")
    val referencedWorks: List<String> = emptyList(),
)

@Serializable
private data class OpenAlexAuthorship(
    val author: OpenAlexAuthor? = null,
)

@Serializable
private data class OpenAlexAuthor(
    @SerialName("display_name")
    val displayName: String? = null,
)

private fun OpenAlexWork.toPaper(): Paper? {
    val paperTitle = title?.trim().orEmpty()
    if (paperTitle.isEmpty()) return null

    return Paper(
        id = id.substringAfterLast('/'),
        title = paperTitle,
        authors = authorships.mapNotNull { it.author?.displayName },
        year = publicationYear,
        doi = doi?.substringAfter("doi.org/"),
        source = "OpenAlex",
        sourceUrl = id,
        citationCount = citedByCount,
        referenceCount = referencedWorks.size,
    )
}
