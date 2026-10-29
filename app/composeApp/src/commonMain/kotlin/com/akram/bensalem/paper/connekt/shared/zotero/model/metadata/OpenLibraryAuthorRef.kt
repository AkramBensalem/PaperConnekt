package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class OpenLibraryAuthorRef(val key: String? = null) {
    val id: String? get() = key?.substringAfterLast('/')
}

/*
  "authors": [
    {
      "key": "/authors/OL13695223A"
    },
    {
      "key": "/authors/OL13695224A"
    },
    {
      "key": "/authors/OL3788004A"
    }
  ],
 */