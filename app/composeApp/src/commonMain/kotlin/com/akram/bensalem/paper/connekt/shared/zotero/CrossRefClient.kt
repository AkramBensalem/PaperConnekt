package com.akram.bensalem.paper.connekt.shared.zotero

import com.akram.bensalem.paper.connekt.shared.zotero.exception.CrossRefException
import com.akram.bensalem.paper.connekt.shared.zotero.model.metadata.CrossRefApiMessage
import com.akram.bensalem.paper.connekt.shared.zotero.model.metadata.CrossRefAuthor
import com.akram.bensalem.paper.connekt.shared.zotero.model.metadata.CrossRefSearchResponse
import com.akram.bensalem.paper.connekt.shared.zotero.model.metadata.CrossRefWork
import com.akram.bensalem.paper.connekt.shared.zotero.model.metadata.OpenLibraryBook
import com.akram.bensalem.paper.connekt.utils.PaperIdentifier
import com.akram.bensalem.paper.connekt.utils.PaperIdentifierType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class CrossRefClient(
    private val userAgent: String = "PaperConnekt/1.0 (mailto:unknown@example.com)",
) {

    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient {
        install(ContentNegotiation) { json(this@CrossRefClient.json) }
        expectSuccess = false
        install(HttpTimeout) { requestTimeoutMillis = 15000; connectTimeoutMillis = 8000 }
        defaultRequest {
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.UserAgent, userAgent)
        }
    }

    suspend fun getReference(identifier: String): CrossRefWork {
        val cleaned = identifier.trim()
        val type = PaperIdentifier.detectType(cleaned)
        println("type is: $type")
        val meta: CrossRefWork = when (type) {
            PaperIdentifierType.DOI -> fetchByDoi(cleanDoi(cleaned))
            PaperIdentifierType.ARXIV -> fetchByArxiv(cleanArxivId(cleaned))
            PaperIdentifierType.ISBN -> fetchByIsbn(cleanIsbn(cleaned))
            /*            PaperIdentifierType.PMID -> fetchByPmid(cleanPmid(cleaned))*/
            PaperIdentifierType.URL -> resolveFromUrl(cleaned)
            else -> throw CrossRefException(cleaned, status = HttpStatusCode.NotFound)
        }
        return meta
    }

    /** Performs a CrossRef free-text search and returns mapped references (journal articles, etc.). */
    suspend fun searchCrossRef(query: String, limit: Int = 20): List<ReferenceMetadata> {
        if (query.isBlank()) return emptyList()
        val resp = http.get("https://api.crossref.org/works") {
            parameter("query", query)
            parameter("rows", limit.coerceIn(1, 100))
        }
        if (resp.status != HttpStatusCode.OK) return emptyList()
        val body: CrossRefSearchResponse =
            runCatching { resp.body<CrossRefSearchResponse>() }.getOrElse { return emptyList() }
        val items = body.message?.items ?: return emptyList()
        return items.map { mapCrossRefWork(it, it.DOI ?: it.title.firstOrNull() ?: "") }
    }

    /** Fetch BibTeX for a DOI (if available). */
    suspend fun fetchBibTeX(doi: String): String? {
        val realDoi = cleanDoi(doi)
        val resp = http.get("https://doi.org/$realDoi") {
            header(HttpHeaders.Accept, "application/x-bibtex")
        }
        return if (resp.status == HttpStatusCode.OK) runCatching { resp.bodyAsText() }.getOrNull() else null
    }

    // -------------------------------- DOI (CrossRef) --------------------------------

    private suspend fun fetchByDoi(doi: String): CrossRefWork {
        val resp = http.get("https://api.crossref.org/works/$doi")
        if (resp.status != HttpStatusCode.OK) {
            throw CrossRefException(status = resp.status, message = "nikek 1")
        }
        val obj: CrossRefApiMessage = runCatching { resp.body<CrossRefApiMessage>() }
            .getOrElse {
                throw CrossRefException(status = resp.status, message = "nikek 2 ${it.message}")
            }
        val work = obj.message ?: throw CrossRefException(status = resp.status, message = "nikek 3")
        return work
    }

    private fun mapCrossRefWork(w: CrossRefWork, identifier: String): ReferenceMetadata {
        val dateParts = w.issued?.dateParts?.firstOrNull()
        val date = dateParts?.let { parts ->
            when (parts.size) {
                3 -> parts[0].toString().padStart(4, '0') + "-" + parts[1].toString()
                    .padStart(2, '0') + "-" + parts[2].toString().padStart(2, '0')

                2 -> parts[0].toString().padStart(4, '0') + "-" + parts[1].toString().padStart(2, '0')
                1 -> parts[0].toString().padStart(4, '0')
                else -> null
            }
        }
        val abstractClean = w.crossRefAbstract?.replace("<jats:p>", "")?.replace("</jats:p>", "")?.trim()
        return ReferenceMetadata(
            identifier = identifier,
            identifierType = "doi",
            title = w.title.firstOrNull() ?: "",
            authors = w.author.map { listOfNotNull(it.given, it.family).joinToString(" ").trim() }
                .filter { it.isNotBlank() }.map(::normalizeAuthor),
            abstract = abstractClean,
            publicationName = w.containerTitle.firstOrNull(),
            publicationDate = date,
            volume = w.volume,
            issue = w.issue,
            pages = w.page,
            doi = w.DOI,
            url = w.URL,
            keywords = w.subject
        )
    }

    internal fun cleanDoi(raw: String): String = raw.trim().removePrefix("doi:").removePrefix("DOI:")
    // -------------------------------- arXiv --------------------------------

    private fun extractArxivTitle(xml: String): String {
        val matches = Regex("<title>([\\s\\S]*?)</title>")
            .findAll(xml)
            .map { it.groupValues[1].trim().lines().joinToString(" ") { ln -> ln.trim() } }
            .toList()

        val title = if (matches.size > 1) matches[1] else matches.firstOrNull().orEmpty()

        return title
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("\\s{2,}".toRegex(), " ")
    }

    fun extract(tag: String, xml: String): String? =
        Regex("(?s)<$tag>(.*?)</$tag>").find(xml)?.groupValues?.get(1)?.trim()

    private suspend fun fetchByArxiv(arxivId: String): CrossRefWork {
        val resp = http.get("https://export.arxiv.org/api/query") { parameter("id_list", arxivId) }
        if (resp.status != HttpStatusCode.OK) throw CrossRefException(status = resp.status, message = "nikek 1")
        val xml = runCatching { resp.bodyAsText() }.getOrElse {
            throw CrossRefException(
                status = resp.status,
                message = "nikek 2"
            )
        }
        println(xml)
        val doi = Regex("<arxiv:doi>(.*?)</arxiv:doi>").find(xml)?.groupValues?.get(1)?.trim()

        if (doi != null && doi.isNotEmpty()) return fetchByDoi(doi)
        val title = extractArxivTitle(xml)

        val authors = Regex("<author>\\s*<name>(.*?)</name>\\s*</author>").findAll(xml)
            .map { normalizeAuthor(it.groupValues[1].trim()) }.toList()
            .map { CrossRefAuthor(family = it) }

        val abstract  = extract("summary", xml)
        return CrossRefWork(
            DOI = doi,
            title = listOf(title),
            author = authors,
            crossRefAbstract =  abstract
        )
    }

    // -------------------------------- PubMed (PMID) --------------------------------

    private suspend fun fetchByPmid(pmid: String): ReferenceMetadata {
        val url = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"
        val resp = http.get(url) {
            parameter("db", "pubmed")
            parameter("id", pmid)
            parameter("retmode", "json")
        }
        if (resp.status != HttpStatusCode.OK) return ReferenceMetadata(identifier = pmid, identifierType = "pmid")
        val raw = runCatching { resp.bodyAsText() }.getOrElse {
            return ReferenceMetadata(
                identifier = pmid,
                identifierType = "pmid"
            )
        }
        val root = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrElse {
            return ReferenceMetadata(
                identifier = pmid,
                identifierType = "pmid",
                rawSource = raw
            )
        }
        val result = root["result"]?.jsonObject ?: return ReferenceMetadata(
            identifier = pmid,
            identifierType = "pmid",
            rawSource = raw
        )
        val uidsArray = result["uids"]?.jsonArray
        val uid = uidsArray?.firstOrNull()?.jsonPrimitive?.content ?: pmid
        val article = result[uid]?.jsonObject ?: return ReferenceMetadata(
            identifier = pmid,
            identifierType = "pmid",
            rawSource = raw
        )
        val title = article["title"]?.jsonPrimitive?.content ?: ""
        val source = article["source"]?.jsonPrimitive?.content
        val pubdate = article["pubdate"]?.jsonPrimitive?.content
        val elocation = article["elocationid"]?.jsonPrimitive?.content // may contain doi: 10.xxxx
        val doi = elocation?.let { Regex("10\\.[^\\s]+").find(it)?.value }
        val authorsArray = article["authors"]
        val authors =
            authorsArray?.jsonArray?.mapNotNull { it.jsonObject["name"]?.jsonPrimitive?.content?.let(::normalizeAuthor) }
                ?: emptyList()
        return ReferenceMetadata(
            identifier = pmid,
            identifierType = "pmid",
            title = title,
            authors = authors,
            publicationName = source,
            publicationDate = pubdate,
            doi = doi,
            url = "https://pubmed.ncbi.nlm.nih.gov/$pmid/",
            rawSource = raw
        )
    }

    // -------------------------------- ISBN (OpenLibrary) --------------------------------

    private suspend fun fetchByIsbn(isbn: String): CrossRefWork {
        val resp = http.get("https://openlibrary.org/isbn/$isbn.json")
        if (resp.status != HttpStatusCode.OK) throw CrossRefException(status = resp.status, message = "nikek 1")
        val book: OpenLibraryBook = runCatching { resp.body<OpenLibraryBook>() }.getOrElse {
            throw CrossRefException(status = resp.status, message = "nikek 2")
        }
        val title = book.title.orEmpty()
        val authors: List<CrossRefAuthor> = book.authors.mapNotNull{it.key}.map{CrossRefAuthor(family = it)}
        return CrossRefWork(
            DOI = null,
            title = if (title.isNotBlank()) listOf(title) else emptyList(),
            author = authors,
        )
    }


    private fun cleanArxivId(raw: String): String = raw.trim().removePrefix("arxiv:").removePrefix("ArXiv:").lowercase()
    private fun cleanPmid(raw: String): String = raw.removePrefix("pmid:").removePrefix("PMID:").trim()
    private fun cleanIsbn(raw: String): String = raw.filter { it.isDigit() || it == 'X' || it == 'x' }

    // -------------------------------- URL (heuristic) --------------------------------

    private suspend fun resolveFromUrl(url: String): CrossRefWork {
        val doiMatch = Regex("doi.org/(10\\.[^#?\\s]+)").find(url)?.groupValues?.getOrNull(1)
        if (doiMatch != null) return fetchByDoi(doiMatch)
        throw CrossRefException(message = "nik mok", status = HttpStatusCode.NotFound)
    }

    fun close() {
        http.close()
    }

    // -------------------------------- Utils --------------------------------

    private fun normalizeAuthor(a: String): String = a.trim().replace("  ", " ").removeSuffix(".")
}