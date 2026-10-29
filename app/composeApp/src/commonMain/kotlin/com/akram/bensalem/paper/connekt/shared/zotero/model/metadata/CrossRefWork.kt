package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class CrossRefWork(

    @SerialName("DOI") val DOI: String? = null,
    @SerialName("publisher") val publisher: String? = null,
    @SerialName("type") val type: String? = null,


    @SerialName("title") val title: List<String> = emptyList(),
    @SerialName("author") val author: List<CrossRefAuthor> = emptyList(),
    @SerialName("issued") val issued: CrossRefIssued? = null,
    @SerialName("issue") val issue: String? = null,

    @SerialName("page") val page: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("article-number") val articleNumber: String? = null,

    @SerialName("prefix") val prefix: String? = null,
    @SerialName("volume") val volume: String? = null,
    @SerialName("member") val member: String? = null,

    @SerialName("URL") val URL: String? = null,
    @SerialName("subject") val subject: List<String> = emptyList(),
    @SerialName("language") val language: String? = null,
    @SerialName("original-title") val originalTitle: List<String> = emptyList(),

    @SerialName("subtitle") val subtitle: List<String> = emptyList(),
    @SerialName("short-title") val shortTitle: List<String> = emptyList(),
    @SerialName("container-title") val containerTitle: List<String> = emptyList(),

    @SerialName("score") val score: Int? = null,

    @SerialName("ISSN") val issn: List<String> = emptyList(),
    @SerialName("alternative-id") val alternativeId: List<String> = emptyList(),

    @SerialName("special_numbering") val specialNumbering: String? = null,

    @SerialName("is-referenced-by-count") val isReferencedByCount: Int? = null,

    @SerialName("reference-count") val referenceCount: Int? = 0,
    @SerialName("references-count") val referencesCount: Int? = null,


    @SerialName("abstract") val crossRefAbstract: String? = null,
    @SerialName("reference") val reference: List<CrossRefReference> = emptyList(),
)

/*
    "created": {
      "date-parts": [[2025,11,11]],
      "date-time": "2025-11-11T23:16:52Z",
      "timestamp": 1762903012000
    },
    "resource": {
      "primary": {
        "URL": "https://linkinghub.elsevier.com/retrieve/pii/S1568494625015418"
      }
    },
    "relation": {},
    "issn-type": [
      {
        "type": "print",
        "value": "1568-4946"
      }
    ],
 */