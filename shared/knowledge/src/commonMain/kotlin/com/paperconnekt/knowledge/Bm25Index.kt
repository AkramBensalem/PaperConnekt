package com.paperconnekt.knowledge

import kotlin.math.ln

/** Okapi BM25 over pre-tokenized documents. Immutable after construction, so it is safe to query from any thread. */
class Bm25Index(documents: List<List<String>>, private val k1: Double = 1.2, private val b: Double = 0.75) {
    private val frequencies: List<Map<String, Int>> = documents.map { terms -> terms.groupingBy { it }.eachCount() }
    private val lengths: IntArray = IntArray(documents.size) { documents[it].size }
    private val averageLength: Double = lengths.average().takeIf { it > 0.0 } ?: 1.0
    private val idf: Map<String, Double> = buildMap {
        val documentFrequency = HashMap<String, Int>()
        frequencies.forEach { document -> document.keys.forEach { documentFrequency[it] = (documentFrequency[it] ?: 0) + 1 } }
        val count = documents.size.toDouble()
        documentFrequency.forEach { (term, frequency) -> put(term, ln(1.0 + (count - frequency + 0.5) / (frequency + 0.5))) }
    }

    val size: Int get() = frequencies.size

    /** Score of every document for [queryTerms] together with the query terms each document contains. */
    fun score(queryTerms: Collection<String>): List<Pair<Double, List<String>>> {
        val unique = queryTerms.distinct()
        return frequencies.mapIndexed { index, document ->
            var score = 0.0
            val matched = ArrayList<String>()
            for (term in unique) {
                val frequency = document[term] ?: continue
                matched += term
                val norm = frequency + k1 * (1 - b + b * lengths[index] / averageLength)
                score += (idf[term] ?: 0.0) * frequency * (k1 + 1) / norm
            }
            score to matched
        }
    }
}
