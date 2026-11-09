package com.paperconnekt.app.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.paperconnekt.app.resources.Res
import com.paperconnekt.app.resources.appearance
import com.paperconnekt.app.resources.arabic
import com.paperconnekt.app.resources.back
import com.paperconnekt.app.resources.continue_action
import com.paperconnekt.app.resources.create_workspace
import com.paperconnekt.app.resources.dark
import com.paperconnekt.app.resources.filled_account_tree
import com.paperconnekt.app.resources.filled_auto_stories
import com.paperconnekt.app.resources.filled_arrow_forward
import com.paperconnekt.app.resources.filled_check_circle
import com.paperconnekt.app.resources.filled_chevron_left
import com.paperconnekt.app.resources.filled_folder
import com.paperconnekt.app.resources.filled_smart_toy
import com.paperconnekt.app.resources.french
import com.paperconnekt.app.resources.english
import com.paperconnekt.app.resources.follow_device_appearance
import com.paperconnekt.app.resources.keep_zotero_optional
import com.paperconnekt.app.resources.language
import com.paperconnekt.app.resources.light
import com.paperconnekt.app.resources.lock
import com.paperconnekt.app.resources.nav_assistant
import com.paperconnekt.app.resources.nav_graph
import com.paperconnekt.app.resources.nav_library
import com.paperconnekt.app.resources.onboarding_description
import com.paperconnekt.app.resources.onboarding_feature_assistant
import com.paperconnekt.app.resources.onboarding_feature_graph
import com.paperconnekt.app.resources.onboarding_feature_library
import com.paperconnekt.app.resources.undraw_ai_personal_assistant
import com.paperconnekt.app.resources.undraw_connect_the_dots
import com.paperconnekt.app.resources.undraw_reading
import com.paperconnekt.app.resources.onboarding_prepare_error
import com.paperconnekt.app.resources.onboarding_preferences_description
import com.paperconnekt.app.resources.onboarding_preferences_title
import com.paperconnekt.app.resources.onboarding_preference_save_error
import com.paperconnekt.app.resources.onboarding_setup_label
import com.paperconnekt.app.resources.onboarding_storage_description
import com.paperconnekt.app.resources.onboarding_storage_local
import com.paperconnekt.app.resources.onboarding_storage_private
import com.paperconnekt.app.resources.onboarding_storage_ready
import com.paperconnekt.app.resources.onboarding_storage_title
import com.paperconnekt.app.resources.onboarding_storage_optional
import com.paperconnekt.app.resources.onboarding_workspace_description
import com.paperconnekt.app.resources.onboarding_workspace_title
import com.paperconnekt.app.resources.optional_tools
import com.paperconnekt.app.resources.optional_tools_description
import com.paperconnekt.app.resources.system_default
import com.paperconnekt.app.resources.use_arabic
import com.paperconnekt.app.resources.use_dark_palette
import com.paperconnekt.app.resources.use_english
import com.paperconnekt.app.resources.use_french
import com.paperconnekt.app.resources.use_light_palette
import com.paperconnekt.app.resources.welcome_to_paperconnekt
import com.paperconnekt.app.resources.workspace_name
import com.paperconnekt.app.resources.zotero_enabled
import com.paperconnekt.domain.model.LanguagePreference
import com.paperconnekt.domain.model.ThemePreference
import com.paperconnekt.storage.LocalResearchStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingScreen(
    store: LocalResearchStore,
    onFinished: () -> Unit,
) {
    val settings by store.settings.collectAsState()
    val storageReady by store.isInitialized.collectAsState()
    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var workspaceName by remember { mutableStateOf(settings.workspaceName) }
    var selectedLanguage by remember { mutableStateOf(settings.selectedLanguage) }
    var selectedTheme by remember { mutableStateOf(settings.selectedTheme) }
    var zoteroEnabled by remember { mutableStateOf(settings.zoteroEnabled) }
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val steps = OnboardingStep.values()
    val stepIndex = steps.indexOf(step)
    val preferenceSaveError = stringResource(Res.string.onboarding_preference_save_error)
    val prepareError = stringResource(Res.string.onboarding_prepare_error)

    fun savePreference(update: (com.paperconnekt.domain.model.AppSettings) -> com.paperconnekt.domain.model.AppSettings) {
        scope.launch {
            try {
                store.updateSettings(update)
                saveError = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                saveError = error.message ?: preferenceSaveError
            }
        }
    }

    fun finish() {
        scope.launch {
            isSaving = true
            saveError = null
            try {
                store.updateSettings {
                    it.copy(
                        onboardingCompleted = true,
                        workspaceName = workspaceName.trim().ifEmpty { "My research" },
                        zoteroEnabled = zoteroEnabled,
                        selectedLanguage = selectedLanguage,
                        selectedTheme = selectedTheme,
                    )
                }
                onFinished()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                saveError = error.message ?: prepareError
            } finally {
                isSaving = false
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            val wide = maxWidth >= 900.dp
            val horizontalPadding = if (wide) 48.dp else 20.dp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = if (wide) 36.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                OnboardingHeader()
                ProgressHeader(currentStep = stepIndex, totalSteps = steps.size)

                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        OnboardingHero(modifier = Modifier.weight(0.9f))
                        OnboardingCard(
                            modifier = Modifier.weight(1.1f),
                            step = step,
                            workspaceName = workspaceName,
                            selectedLanguage = selectedLanguage,
                            selectedTheme = selectedTheme,
                            zoteroEnabled = zoteroEnabled,
                            storageReady = storageReady,
                            onLanguageSelected = {
                                selectedLanguage = it
                            },
                            onThemeSelected = {
                                selectedTheme = it
                                savePreference { settings -> settings.copy(selectedTheme = it) }
                            },
                            onWorkspaceNameChanged = { workspaceName = it },
                            onZoteroEnabledChanged = { zoteroEnabled = it },
                            saveError = saveError,
                            isSaving = isSaving,
                            onBack = { step = steps[(stepIndex - 1).coerceAtLeast(0)] },
                            onNext = {
                                if (step == OnboardingStep.STORAGE) finish()
                                else step = steps[(stepIndex + 1).coerceAtMost(steps.lastIndex)]
                            },
                        )
                    }
                } else {
                    OnboardingHero()
                    OnboardingCard(
                        step = step,
                        workspaceName = workspaceName,
                        selectedLanguage = selectedLanguage,
                        selectedTheme = selectedTheme,
                        zoteroEnabled = zoteroEnabled,
                        storageReady = storageReady,
                        onLanguageSelected = {
                            selectedLanguage = it
                        },
                        onThemeSelected = {
                            selectedTheme = it
                            savePreference { settings -> settings.copy(selectedTheme = it) }
                        },
                        onWorkspaceNameChanged = { workspaceName = it },
                        onZoteroEnabledChanged = { zoteroEnabled = it },
                        saveError = saveError,
                        isSaving = isSaving,
                        onBack = { step = steps[(stepIndex - 1).coerceAtLeast(0)] },
                        onNext = {
                            if (step == OnboardingStep.STORAGE) finish()
                            else step = steps[(stepIndex + 1).coerceAtMost(steps.lastIndex)]
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                painter = painterResource(Res.drawable.filled_auto_stories),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(10.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = "PaperConnekt",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.onboarding_setup_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressHeader(
    currentStep: Int,
    totalSteps: Int,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (index <= currentStep) 54.dp else 34.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        },
                    ),
            )
        }
    }
}

@Composable
private fun OnboardingHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primary,
        ) {
            Icon(
                painter = painterResource(Res.drawable.filled_auto_stories),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp),
            )
        }
        Image(
            painter = painterResource(Res.drawable.undraw_reading),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp),
        )
        Text(
            text = stringResource(Res.string.welcome_to_paperconnekt),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Text(
            text = stringResource(Res.string.onboarding_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f),
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureRow(
                icon = Res.drawable.filled_auto_stories,
                title = stringResource(Res.string.nav_library),
                description = stringResource(Res.string.onboarding_feature_library),
            )
            FeatureRow(
                icon = Res.drawable.filled_account_tree,
                title = stringResource(Res.string.nav_graph),
                description = stringResource(Res.string.onboarding_feature_graph),
            )
            FeatureRow(
                icon = Res.drawable.filled_smart_toy,
                title = stringResource(Res.string.nav_assistant),
                description = stringResource(Res.string.onboarding_feature_assistant),
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: DrawableResource,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(34.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f),
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp),
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun OnboardingCard(
    modifier: Modifier = Modifier,
    step: OnboardingStep,
    workspaceName: String,
    selectedLanguage: LanguagePreference,
    selectedTheme: ThemePreference,
    zoteroEnabled: Boolean,
    storageReady: Boolean,
    onLanguageSelected: (LanguagePreference) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
    onWorkspaceNameChanged: (String) -> Unit,
    onZoteroEnabledChanged: (Boolean) -> Unit,
    saveError: String?,
    isSaving: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when (step) {
                OnboardingStep.WELCOME -> WelcomeStep()
                OnboardingStep.PREFERENCES -> PreferencesStep(
                    selectedLanguage = selectedLanguage,
                    selectedTheme = selectedTheme,
                    onLanguageSelected = onLanguageSelected,
                    onThemeSelected = onThemeSelected,
                )
                OnboardingStep.WORKSPACE -> WorkspaceStep(
                    workspaceName = workspaceName,
                    onWorkspaceNameChanged = onWorkspaceNameChanged,
                )
                OnboardingStep.OPTIONAL_TOOLS -> OptionalToolsStep(
                    zoteroEnabled = zoteroEnabled,
                    onZoteroEnabledChanged = onZoteroEnabledChanged,
                )
                OnboardingStep.STORAGE -> StorageStep(storageReady = storageReady)
            }

            saveError?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step != OnboardingStep.WELCOME) {
                    OutlinedButton(
                        onClick = onBack,
                        enabled = !isSaving,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.filled_chevron_left),
                            contentDescription = null,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(Res.string.back))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onNext,
                    enabled = !isSaving && (step != OnboardingStep.STORAGE || storageReady),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            stringResource(
                                if (step == OnboardingStep.STORAGE) {
                                    Res.string.create_workspace
                                } else {
                                    Res.string.continue_action
                                },
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            painter = painterResource(
                                if (step == OnboardingStep.STORAGE) {
                                    Res.drawable.filled_check_circle
                                } else {
                                    Res.drawable.filled_arrow_forward
                                },
                            ),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep() {
    StepHeading(
        title = stringResource(Res.string.welcome_to_paperconnekt),
        description = stringResource(Res.string.onboarding_description),
    )
}

@Composable
private fun PreferencesStep(
    selectedLanguage: LanguagePreference,
    selectedTheme: ThemePreference,
    onLanguageSelected: (LanguagePreference) -> Unit,
    onThemeSelected: (ThemePreference) -> Unit,
) {
    StepHeading(
        title = stringResource(Res.string.onboarding_preferences_title),
        description = stringResource(Res.string.onboarding_preferences_description),
    )
    PreferenceGroup(
        title = stringResource(Res.string.language),
        options = LanguagePreference.values().toList(),
        selected = selectedLanguage,
        label = { it.localizedLabel() },
        description = { it.localizedDescription() },
        onSelected = onLanguageSelected,
    )
    PreferenceGroup(
        title = stringResource(Res.string.appearance),
        options = ThemePreference.values().toList(),
        selected = selectedTheme,
        label = { it.localizedLabel() },
        description = { it.localizedDescription() },
        onSelected = onThemeSelected,
    )
}

@Composable
private fun <T> PreferenceGroup(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    description: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        options.forEach { option ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onSelected(option) }
                    .border(
                        BorderStroke(
                            1.dp,
                            if (option == selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ),
                        MaterialTheme.shapes.medium,
                    ),
                color = if (option == selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = option == selected,
                        onClick = { onSelected(option) },
                    )
                    Column {
                        Text(label(option), style = MaterialTheme.typography.titleSmall)
                        Text(
                            description(option),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkspaceStep(
    workspaceName: String,
    onWorkspaceNameChanged: (String) -> Unit,
) {
    StepHeading(
        title = stringResource(Res.string.onboarding_workspace_title),
        description = stringResource(Res.string.onboarding_workspace_description),
    )
    androidx.compose.material3.OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = workspaceName,
        onValueChange = onWorkspaceNameChanged,
        label = { Text(stringResource(Res.string.workspace_name)) },
        singleLine = true,
    )
}

@Composable
private fun OptionalToolsStep(
    zoteroEnabled: Boolean,
    onZoteroEnabledChanged: (Boolean) -> Unit,
) {
    StepHeading(
        title = stringResource(Res.string.optional_tools),
        description = stringResource(Res.string.optional_tools_description),
    )
    Image(
        painter = painterResource(Res.drawable.undraw_ai_personal_assistant),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable { onZoteroEnabledChanged(!zoteroEnabled) },
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(Res.drawable.filled_folder),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Zotero", style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        if (zoteroEnabled) Res.string.zotero_enabled else Res.string.keep_zotero_optional,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = zoteroEnabled,
                onClick = { onZoteroEnabledChanged(!zoteroEnabled) },
            )
        }
    }
}

@Composable
private fun StorageStep(storageReady: Boolean) {
    StepHeading(
        title = stringResource(Res.string.onboarding_storage_title),
        description = stringResource(Res.string.onboarding_storage_description),
    )
    Image(
        painter = painterResource(Res.drawable.undraw_connect_the_dots),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StorageCheckRow(
            icon = Res.drawable.filled_check_circle,
            text = stringResource(Res.string.onboarding_storage_ready),
            ready = storageReady,
        )
        StorageCheckRow(
            icon = Res.drawable.lock,
            text = stringResource(Res.string.onboarding_storage_private),
            ready = true,
        )
        StorageCheckRow(
            icon = Res.drawable.filled_folder,
            text = stringResource(Res.string.onboarding_storage_local),
            ready = true,
        )
        Text(
            stringResource(Res.string.onboarding_storage_optional),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StorageCheckRow(
    icon: DrawableResource,
    text: String,
    ready: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun StepHeading(
    title: String,
    description: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Text(
            description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LanguagePreference.localizedLabel(): String = stringResource(
    when (this) {
        LanguagePreference.ENGLISH -> Res.string.english
        LanguagePreference.FRENCH -> Res.string.french
        LanguagePreference.ARABIC -> Res.string.arabic
    },
)

@Composable
private fun LanguagePreference.localizedDescription(): String = stringResource(
    when (this) {
        LanguagePreference.ENGLISH -> Res.string.use_english
        LanguagePreference.FRENCH -> Res.string.use_french
        LanguagePreference.ARABIC -> Res.string.use_arabic
    },
)

@Composable
private fun ThemePreference.localizedLabel(): String = stringResource(
    when (this) {
        ThemePreference.SYSTEM -> Res.string.system_default
        ThemePreference.LIGHT -> Res.string.light
        ThemePreference.DARK -> Res.string.dark
    },
)

@Composable
private fun ThemePreference.localizedDescription(): String = stringResource(
    when (this) {
        ThemePreference.SYSTEM -> Res.string.follow_device_appearance
        ThemePreference.LIGHT -> Res.string.use_light_palette
        ThemePreference.DARK -> Res.string.use_dark_palette
    },
)
