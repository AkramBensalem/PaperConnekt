package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CrossRefPublished(
    @SerialName("date-parts") val dateParts: List<List<Int>> = emptyList()
)

/*
    "published": {
      "date-parts": [
        [
          2026,
          1
        ]
      ]
    },
 */