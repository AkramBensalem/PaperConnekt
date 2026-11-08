package com.akram.bensalem.paper.connekt.feature.assistant.component

import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.filled_picture_as_pdf
import com.paperconnekt.app.resources.pdf_icon
import com.paperconnekt.app.resources.pdf_viewer_placeholder
import com.paperconnekt.app.resources.paper_url
import com.paperconnekt.app.resources.viewing_paper
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.akram.bensalem.bistosh.core.designsystem.component.interactivePointer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akram.bensalem.paper.connekt.models.Paper
import com.paperconnekt.app.resources.open_external_viewer

@Composable
fun PdfViewer(paper: Paper) {
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(Res.drawable.filled_picture_as_pdf),
                contentDescription = stringResource(Res.string.pdf_icon),
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.pdf_viewer_placeholder),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.viewing_paper, paper.title),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.paper_url, paper.url.orEmpty()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { /* Open External PDF if needed */ }, modifier = Modifier.interactivePointer()) {
                Text(stringResource(Res.string.open_external_viewer))
            }
        }
    }
}
