package com.akram.bensalem.paper.connekt.shared.dto.request.zotero

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Zotero sometimes uses `false` instead of `null` for optional string keys like `parentCollection`.
 * This serializer maps `"someKey"` -> "someKey" and `false` -> null.
 * When encoding, null is emitted as JSON boolean `false` (which Zotero accepts),
 * and non-null strings are encoded as JSON strings.
 */
object StringOrFalseAsNullSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("StringOrFalseAsNull", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        return if (decoder is JsonDecoder) {
            val elem = decoder.decodeJsonElement()
            when (elem) {
                is JsonPrimitive -> {
                    when {
                        elem.isString -> elem.content
                        elem.booleanOrNull == false -> null
                        elem.isString.not() && elem.booleanOrNull == null -> null
                        else -> null
                    }
                }
                else -> null
            }
        } else {
            // Fallback, treat as nullable string
            val s = decoder.decodeString()
            if (s.isBlank()) null else s
        }
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (encoder is JsonEncoder) {
            if (value == null) {
                encoder.encodeBoolean(false)
            } else {
                encoder.encodeString(value)
            }
        } else {
            if (value == null) encoder.encodeString("") else encoder.encodeString(value)
        }
    }
}
