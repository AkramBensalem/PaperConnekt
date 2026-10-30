package com.paperconnekt.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.lock
import com.paperconnekt.app.resources.filled_check_circle
import com.paperconnekt.app.resources.outlined_open_in_new
import com.paperconnekt.app.resources.visibility
import com.paperconnekt.app.resources.visibility_off
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun ProviderCredentialCard(
    providerName: String,
    description: String,
    configured: Boolean,
    draft: String,
    onDraftChange: (String) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onValidate: () -> Unit,
    onOpenKeyPage: (() -> Unit)? = null,
    feedback: String? = null,
    feedbackIsError: Boolean = true,
    enabled: Boolean = true,
    providerIcon: DrawableResource? = null,
    providerIconBackground: Color? = null,
) {
    var revealed by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CredentialIcon(
                    contentDescription = if (providerIcon == null) {
                        "$providerName credential"
                    } else {
                        "$providerName logo"
                    },
                    providerIcon = providerIcon,
                    providerIconBackground = providerIconBackground,
                )
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
                    text = if (configured) "Key saved" else "Not configured",
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
                        Icon(
                            painter = painterResource(
                                if (revealed) Res.drawable.visibility_off else Res.drawable.visibility,
                            ),
                            contentDescription = null,
                        )
                    }
                },
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                Button(
                    enabled = enabled && draft.isNotBlank(),
                    onClick = onSave,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = null)
                        Text("Check & save")
                    }
                }
                TextButton(
                    enabled = enabled && configured,
                    onClick = onClear,
                ) {
                    Text("Clear")
                }
                TextButton(
                    enabled = enabled && configured,
                    onClick = onValidate,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = null)
                        Text("Check key")
                    }
                }
                onOpenKeyPage?.let { open ->
                    TextButton(
                        onClick = open,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(Res.drawable.outlined_open_in_new), contentDescription = null)
                            Text("Get a key")
                        }
                    }
                }
            }
            feedback?.let { message ->
                Text(
                    text = message,
                    color = if (feedbackIsError) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
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
private fun CredentialIcon(
    contentDescription: String,
    providerIcon: DrawableResource?,
    providerIconBackground: Color?,
) {
    if (providerIcon == null) {
        Icon(
            painter = painterResource(Res.drawable.lock),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    } else {
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 28.dp)
                .then(
                    if (providerIconBackground == null) {
                        Modifier
                    } else {
                        Modifier
                            .background(providerIconBackground, RoundedCornerShape(6.dp))
                            .padding(4.dp)
                    },
                ),
        ) {
            Image(
                painter = painterResource(providerIcon),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
