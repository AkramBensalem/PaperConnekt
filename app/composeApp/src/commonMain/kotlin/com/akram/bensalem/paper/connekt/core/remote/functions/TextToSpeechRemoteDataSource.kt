package com.akram.bensalem.paper.connekt.core.remote.functions

import com.akram.bensalem.paper.connekt.core.remote.functions.model.TtsAudio
import com.akram.bensalem.paper.connekt.core.remote.util.RemoteResult

interface TextToSpeechRemoteDataSource {
    suspend fun generate(
        text: String,
        languageCode: String,
        kind: String,
        rootId: String,
        exampleId: String,
        ssml: String? = null,
    ): RemoteResult<TtsAudio>
}
