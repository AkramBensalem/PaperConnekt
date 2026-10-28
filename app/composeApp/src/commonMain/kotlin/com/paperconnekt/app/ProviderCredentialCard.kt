package com.paperconnekt.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun ProviderCredentialCard(
    providerName: String,
    description: String,
    configured: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    enabled: Boolean = true,
) {
    var revealed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CredentialIcon("$providerName credential")
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(providerName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Text(
                    text = if (configured) "Configured" else "Not configured",
                    color = if (configured) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = draft,
                onValueChange = onDraftChange,
                enabled = enabled,
                label = { Text(if (configured) "Replace API key" else "API key") },
                placeholder = { Text("Paste a key to store locally") },
                singleLine = true,
                visualTransformation = if (revealed) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = { revealed = !revealed },
                        enabled = enabled,
                        modifier = Modifier.semantics {
                            contentDescription = if (revealed) {
                                "Hide $providerName API key"
                            } else {
                                "Show $providerName API key"
                            }
                        },
                    ) {
                        Text(if (revealed) "Hide" else "Show")
                    }
                },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = enabled && draft.isNotBlank(),
                    onClick = onSave,
                ) {
                    Text("Save locally")
                }
                TextButton(
                    enabled = enabled && configured,
                    onClick = onClear,
                ) {
                    Text("Clear")
                }
            }

            Text(
                "The saved value is kept in this device's platform secret store and is never shown here.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun CredentialIcon(contentDescription: String) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .semantics { this.contentDescription = contentDescription }
            .size(24.dp),
    ) {
        val stroke = Stroke(width = 1.8.dp.toPx())
        drawCircle(color, center = Offset(7.dp.toPx(), size.height / 2), radius = 4.dp.toPx(), style = stroke)
        drawLine(
            color,
            start = Offset(10.dp.toPx(), size.height / 2),
            end = Offset(21.dp.toPx(), size.height / 2),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
        drawLine(
            color,
            start = Offset(17.dp.toPx(), size.height / 2),
            end = Offset(17.dp.toPx(), size.height / 2 + 4.dp.toPx()),
            strokeWidth = stroke.width,
            cap = StrokeCap.Round,
        )
    }
}
