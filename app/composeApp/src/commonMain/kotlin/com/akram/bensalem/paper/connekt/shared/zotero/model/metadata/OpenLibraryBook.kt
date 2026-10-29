package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import com.akram.bensalem.paper.connekt.shared.zotero.model.metadata.OpenLibraryAuthorRef
import kotlinx.serialization.Serializable

@Serializable
data class OpenLibraryLanguageRef(val key: String? = null) {
    val id: String? get() = key?.substringAfterLast('/')
}

@Serializable
data class OpenLibraryDateRef(
    val type: String? = null,
    val value: String? = null
)

@Serializable
data class OpenLibraryTypeRef(val key: String? = null)

@Serializable
data class OpenLibraryWorkRef(val key: String? = null)

@Serializable
 data class OpenLibraryBook(
    val title: String? = null,
    val subtitle: String? = null,
    val full_title: String? = null,

    val languages : List<OpenLibraryLanguageRef> = emptyList(),
    val authors: List<OpenLibraryAuthorRef> = emptyList(),


    val publish_date: String? = null,
    val publishers: List<String> = emptyList(),

    // fields from sample JSON
    val type: OpenLibraryTypeRef? = null,
    val isbn_13: List<String> = emptyList(),
    val lc_classifications: List<String> = emptyList(),
    val pagination: String? = null,
    val source_records: List<String> = emptyList(),
    val works: List<OpenLibraryWorkRef> = emptyList(),
    val key: String? = null,
    val latest_revision: Int? = null,
    val revision: Int? = null,
    val created: OpenLibraryDateRef? = null,
    val last_modified: OpenLibraryDateRef? = null
     )

/*

{
  "type": {
    "key": "/type/edition"
  },
  "isbn_13": [
    "9783031636455"
  ],
  "lc_classifications": [
    "Q334-342"
  ],
  "pagination": "xii, 459",
  "source_records": [
    "bwb:9783031636455"
  ],
  "works": [
    {
      "key": "/works/OL27322959W"
    }
  ],
  "key": "/books/OL51797163M",
  "latest_revision": 1,
  "revision": 1,
  "created": {
    "type": "/type/datetime",
    "value": "2024-06-17T14:25:19.286331"
  },
  "last_modified": {
    "type": "/type/datetime",
    "value": "2024-06-17T14:25:19.286331"
  }
}
 */