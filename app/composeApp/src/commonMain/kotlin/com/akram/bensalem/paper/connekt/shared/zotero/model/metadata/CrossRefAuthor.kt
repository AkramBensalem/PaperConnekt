package com.akram.bensalem.paper.connekt.shared.zotero.model.metadata

import kotlinx.serialization.Serializable

@Serializable
public data class CrossRefAuthor(
    val given: String? = null,
    val family: String? = null
)

/*
    "author": [
      {
        "ORCID": "https://orcid.org/0000-0002-7777-0818",
        "authenticated-orcid": false,
        "given": "Wentao",
        "family": "Li",
        "sequence": "first",
        "affiliation": []
      },
      {
        "ORCID": "https://orcid.org/0009-0002-0011-4414",
        "authenticated-orcid": false,
        "given": "Chaojun",
        "family": "Deng",
        "sequence": "additional",
        "affiliation": []
      },
      {
        "given": "Witold",
        "family": "Pedrycz",
        "sequence": "additional",
        "affiliation": []
      },
      {
        "ORCID": "https://orcid.org/0000-0002-3180-7347",
        "authenticated-orcid": false,
        "given": "Weiping",
        "family": "Ding",
        "sequence": "additional",
        "affiliation": []
      },
      {
        "ORCID": "https://orcid.org/0000-0001-6879-5266",
        "authenticated-orcid": false,
        "given": "Xingchen",
        "family": "Hu",
        "sequence": "additional",
        "affiliation": []
      },
      {
        "ORCID": "https://orcid.org/0000-0001-6248-9962",
        "authenticated-orcid": false,
        "given": "Chao",
        "family": "Zhang",
        "sequence": "additional",
        "affiliation": []
      },
      {
        "given": "Tao",
        "family": "Zhan",
        "sequence": "additional",
        "affiliation": []
      }
    ],
 */