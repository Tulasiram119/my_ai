package com.localai.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.localai.app.data.model.InferenceConfig
import com.localai.app.ui.theme.DarkPrimary

@Composable
fun InferenceSettingsDialog(
    config: InferenceConfig,
    onDismiss: () -> Unit,
    onSaveConfig: (InferenceConfig) -> Unit
) {
    var temperature by remember { mutableFloatStateOf(config.temperature) }
    var topK by remember { mutableIntStateOf(config.topK) }
    var maxTokens by remember { mutableIntStateOf(config.maxTokens) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Inference Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Temperature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Temperature",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.2f", temperature),
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkPrimary
                    )
                }
                Slider(
                    value = temperature,
                    onValueChange = { temperature = it },
                    valueRange = 0.1f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = DarkPrimary,
                        activeTrackColor = DarkPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Top-K
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top-K",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$topK",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkPrimary
                    )
                }
                Slider(
                    value = topK.toFloat(),
                    onValueChange = { topK = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = DarkPrimary,
                        activeTrackColor = DarkPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Max Tokens
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Max Tokens",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$maxTokens",
                        style = MaterialTheme.typography.labelSmall,
                        color = DarkPrimary
                    )
                }
                Slider(
                    value = maxTokens.toFloat(),
                    onValueChange = { maxTokens = it.toInt() },
                    valueRange = 64f..2048f,
                    colors = SliderDefaults.colors(
                        thumbColor = DarkPrimary,
                        activeTrackColor = DarkPrimary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSaveConfig(
                        InferenceConfig(
                            maxTokens = maxTokens,
                            temperature = temperature,
                            topK = topK
                        ).validated()
                    )
                    onDismiss()
                }
            ) {
                Text("Save", color = DarkPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
