package com.paperconnekt.storage

import com.paperconnekt.domain.model.GraphConnection
import com.paperconnekt.domain.model.ConnectionType
import com.paperconnekt.domain.model.Paper
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class GraphExpansionDirection {
    REFERENCES,
    CITATIONS,
}

data class OpenAlexGraphExpansion(
    val papers: List<Paper>,
    val connections: List<GraphConnection>,
    val total: Int,
)

/** Loads the citation neighborhood used by the graph canvas. */
class OpenAlexGraphProvider(
    private val client: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    suspend fun expand(
        paperId: String,
        direction: GraphExpansionDirection,
        limit: Int = 50,
    ): OpenAlexGraphExpansion {
        val workId = paperId.substringAfterLast('/').removePrefix("https://openalex.org/")
        require(workId.startsWith("W")) { "This paper does not have an OpenAlex work id." }

        return when (direction) {
            GraphExpansionDirection.REFERENCES -> expandReferences(workId, limit)
            GraphExpansionDirection.CITATIONS -> expandCitations(workId, limit)
        }
    }

    private suspend fun expandReferences(workId: String, limit: Int): OpenAlexGraphExpansion {
        val central = getWork(workId)
        val ids = central.referencedWorks
            .take(limit)
            .map { it.substringAfterLast('/') }
        if (ids.isEmpty()) return OpenAlexGraphExpansion(emptyList(), emptyList(), 0)
        val works = getWorks(ids)
        return OpenAlexGraphExpansion(
            papers = works.mapNotNull { it.toPaper() },
            connections = works.mapNotNull { work ->
                work.toPaper()?.let { GraphConnection(workId, it.id, ConnectionType.CITATION) }
            },
            total = central.referencedWorks.size,
        )
    }

    private suspend fun expandCitations(workId: String, limit: Int): OpenAlexGraphExpansion {
        val response = client.get("https://api.openalex.org/works") {
            parameter("filter", "cites:$workId")
            parameter("per-page", limit.coerceIn(1, 100))
            parameter("select", WORK_SELECT)
        }
        val payload = json.decodeFromString<GraphOpenAlexWorksResponse>(response.bodyAsText())
        return OpenAlexGraphExpansion(
            papers = payload.results.mapNotNull { it.toPaper() },
            connections = payload.results.mapNotNull { work ->
                work.toPaper()?.let { GraphConnection(it.id, workId, ConnectionType.CITATION) }
            },
            total = payload.meta?.count ?: payload.results.size,
        )
    }

    private suspend fun getWork(workId: String): GraphOpenAlexWork =
        json.decodeFromString(client.get("https://api.openalex.org/works/$workId") {
            parameter("select", WORK_SELECT)
        }.bodyAsText())

    private suspend fun getWorks(ids: List<String>): List<GraphOpenAlexWork> {
        val response = client.get("https://api.openalex.org/works") {
            parameter("filter", "openalex_id:${ids.joinToString("|")}")
            parameter("per-page", ids.size.coerceIn(1, 100))
            parameter("select", WORK_SELECT)
        }
        return json.decodeFromString<GraphOpenAlexWorksResponse>(response.bodyAsText()).results
    }

    private companion object {
        const val WORK_SELECT =
            "id,title,publication_year,authorships,doi,open_access,cited_by_count,referenced_works"
    }
}

@Serializable
private data class GraphOpenAlexWorksResponse(
    val meta: GraphOpenAlexMeta? = null,
    val results: List<GraphOpenAlexWork> = emptyList(),
)

@Serializable
private data class GraphOpenAlexMeta(val count: Int? = null)

@Serializable
private data class GraphOpenAlexWork(
    val id: String,
    val title: String? = null,
    @SerialName("publication_year") val publicationYear: Int? = null,
    val authorships: List<GraphOpenAlexAuthorship> = emptyList(),
    val doi: String? = null,
    @SerialName("cited_by_count") val citedByCount: Int? = null,
    @SerialName("referenced_works") val referencedWorks: List<String> = emptyList(),
)

@Serializable
private data class GraphOpenAlexAuthorship(val author: GraphOpenAlexAuthor? = null)

@Serializable
private data class GraphOpenAlexAuthor(@SerialName("display_name") val displayName: String? = null)

private fun GraphOpenAlexWork.toPaper(): Paper? {
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
