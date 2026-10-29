package com.akram.bensalem.paper.connekt.feature.assistant

actual suspend fun openPdfDocument(bytes: ByteArray): PdfDocument = openJvmPdfDocument(bytes)
