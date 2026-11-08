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
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.paperconnekt.app.resources.api_key
import com.paperconnekt.app.resources.check_key
import com.paperconnekt.app.resources.check_save
import com.paperconnekt.app.resources.clear
import com.paperconnekt.app.resources.get_key
import com.paperconnekt.app.resources.key_saved
import com.paperconnekt.app.resources.not_configured
import com.paperconnekt.app.resources.paste_key
import com.paperconnekt.app.resources.replace_api_key
import com.paperconnekt.app.resources.secret_store_note

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
    val keySavedLabel = stringResource(Res.string.key_saved)
    val notConfiguredLabel = stringResource(Res.string.not_configured)
    val apiKeyLabel = stringResource(Res.string.api_key)
    val replaceApiKeyLabel = stringResource(Res.string.replace_api_key)
    val pasteKeyLabel = stringResource(Res.string.paste_key)
    val checkSaveLabel = stringResource(Res.string.check_save)
    val checkKeyLabel = stringResource(Res.string.check_key)
    val getKeyLabel = stringResource(Res.string.get_key)
    val secretStoreNote = stringResource(Res.string.secret_store_note)
    val hideKeyDescription = "Hide $providerName API key"
    val showKeyDescription = "Show $providerName API key"

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
                    text = if (configured) keySavedLabel else notConfiguredLabel,
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
                label = { Text(if (configured) replaceApiKeyLabel else apiKeyLabel) },
                placeholder = { Text(pasteKeyLabel) },
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
                                hideKeyDescription
                            } else {
                                showKeyDescription
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
                        Text(checkSaveLabel)
                    }
                }
                TextButton(
                    enabled = enabled && configured,
                    onClick = onClear,
                ) {
                    Text(stringResource(Res.string.clear))
                }
                TextButton(
                    enabled = enabled && configured,
                    onClick = onValidate,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(painterResource(Res.drawable.filled_check_circle), contentDescription = null)
                        Text(checkKeyLabel)
                    }
                }
                onOpenKeyPage?.let { open ->
                    TextButton(
                        onClick = open,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(painterResource(Res.drawable.outlined_open_in_new), contentDescription = null)
                            Text(getKeyLabel)
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
                secretStoreNote,
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
