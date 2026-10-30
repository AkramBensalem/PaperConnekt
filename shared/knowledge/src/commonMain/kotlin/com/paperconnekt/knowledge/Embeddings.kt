package com.paperconnekt.knowledge

/**
 * Turns texts into embedding vectors. The app wraps a provider client (Koog's Mistral or OpenAI client); this module
 * never depends on one, so hosts such as the MCP server or tests can supply their own.
 */
interface EmbeddingProvider {
    /** Stable identifier of the model, for example `mistral/mistral-embed`; vectors of different models never mix. */
    val modelId: String

    /** Embeds [texts] in order, one vector per text. Implementations batch and rate-limit as their provider needs. */
    suspend fun embed(texts: List<String>): List<FloatArray>
}

/**
 * Persisted chunk vectors of one paper and model, keyed by [contentHash] of the embedded text. Keying by content (not
 * by chunk ID) means a re-chunked or slightly edited paper only re-embeds the passages that actually changed.
 */
interface VectorStore {
    suspend fun load(paperId: String, modelId: String): Map<Long, FloatArray>?
    suspend fun save(paperId: String, modelId: String, vectors: Map<Long, FloatArray>)
}

/** Keeps vectors for the lifetime of the process; used by tests and platforms without file storage. */
class MemoryVectorStore : VectorStore {
    private val entries = HashMap<Pair<String, String>, Map<Long, FloatArray>>()

    override suspend fun load(paperId: String, modelId: String): Map<Long, FloatArray>? = entries[paperId to modelId]

    override suspend fun save(paperId: String, modelId: String, vectors: Map<Long, FloatArray>) {
        entries[paperId to modelId] = vectors.toMap()
    }
}

/** 64-bit FNV-1a hash of [text]; deterministic on every platform, unlike [String.hashCode] guarantees. */
fun contentHash(text: String): Long {
    var hash = -0x340d631b7bdddcdbL
    for (char in text) {
        hash = hash xor (char.code.toLong() and 0xff)
        hash *= 0x100000001b3L
        hash = hash xor (char.code.toLong() ushr 8)
        hash *= 0x100000001b3L
    }
    return hash
}

/**
 * Compact binary form of a vector map: magic `PCV1`, dimension and count as 32-bit integers, then per entry a 64-bit
 * content hash and the float32 components, all little-endian. About 4 KB per chunk for 1024 dimensions.
 */
object VectorCodec {
    private val MAGIC = byteArrayOf('P'.code.toByte(), 'C'.code.toByte(), 'V'.code.toByte(), '1'.code.toByte())

    fun encode(vectors: Map<Long, FloatArray>): ByteArray {
        val dimensions = vectors.values.firstOrNull()?.size ?: 0
        val entries = vectors.filterValues { it.size == dimensions }
        val out = ByteArray(12 + entries.size * (8 + 4 * dimensions))
        MAGIC.copyInto(out)
        var at = 4
        at = out.putInt(at, dimensions)
        at = out.putInt(at, entries.size)
        for ((hash, vector) in entries) {
            at = out.putLong(at, hash)
            for (value in vector) at = out.putInt(at, value.toRawBits())
        }
        return out
    }

    /** Decoded vectors, or null when [bytes] is not a complete vector file (the caller then re-embeds). */
    fun decode(bytes: ByteArray): Map<Long, FloatArray>? {
        if (bytes.size < 12 || !bytes.copyOfRange(0, 4).contentEquals(MAGIC)) return null
        val dimensions = bytes.getInt(4)
        val count = bytes.getInt(8)
        if (dimensions < 0 || count < 0 || bytes.size.toLong() != 12L + count.toLong() * (8 + 4L * dimensions)) return null
        val result = LinkedHashMap<Long, FloatArray>(count)
        var at = 12
        repeat(count) {
            val hash = bytes.getLong(at)
            at += 8
            result[hash] = FloatArray(dimensions) { index -> Float.fromBits(bytes.getInt(at + 4 * index)) }
            at += 4 * dimensions
        }
        return result
    }

    private fun ByteArray.putInt(at: Int, value: Int): Int {
        for (i in 0 until 4) this[at + i] = (value ushr (8 * i)).toByte()
        return at + 4
    }

    private fun ByteArray.putLong(at: Int, value: Long): Int {
        for (i in 0 until 8) this[at + i] = (value ushr (8 * i)).toByte()
        return at + 8
    }

    private fun ByteArray.getInt(at: Int): Int {
        var value = 0
        for (i in 0 until 4) value = value or ((this[at + i].toInt() and 0xff) shl (8 * i))
        return value
    }

    private fun ByteArray.getLong(at: Int): Long {
        var value = 0L
        for (i in 0 until 8) value = value or ((this[at + i].toLong() and 0xff) shl (8 * i))
        return value
    }
}
