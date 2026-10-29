package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.Serializable

@Serializable
data class CrossRefApiMessage(
    val message: CrossRefWork? = null
)

/*
{
  "message": {  }
}
 */