package com.akram.bensalem.paper.connekt.feature.assistant

expect fun saveSpeechPlanJson(fileName: String, json: String): Boolean
expect fun createTtsAudioPlayer(): TtsAudioPlayer
