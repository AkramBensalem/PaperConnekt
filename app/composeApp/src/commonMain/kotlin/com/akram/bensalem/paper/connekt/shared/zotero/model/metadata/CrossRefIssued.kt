package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CrossRefIssued(
    @SerialName("date-parts") val dateParts: List<List<Int>> = emptyList()
)

/*
    "issued": {
      "date-parts": [
        [
          2026,
          1
        ]
      ]
    },
 */