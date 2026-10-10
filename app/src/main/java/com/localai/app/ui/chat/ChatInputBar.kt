package com.localai.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localai.app.R
import com.localai.app.ui.theme.AccentRed
import com.localai.app.ui.theme.DarkPrimary
import com.localai.app.ui.theme.DarkSurface
import com.localai.app.ui.theme.DarkSurfaceVariant

@Composable
fun ChatInputBar(
    isGenerating: Boolean,
    isModelLoaded: Boolean,
    onSendMessage: (String) -> Unit,
    onStopGeneration: () -> Unit,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = {
                    Text(
                        text = if (isModelLoaded) {
                            stringResource(R.string.prompt_placeholder)
                        } else {
                            stringResource(R.string.no_model_selected)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                enabled = isModelLoaded && !isGenerating,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    disabledContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f),
                    focusedBorderColor = DarkPrimary,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 5
            )

            if (isGenerating) {
                IconButton(
                    onClick = onStopGeneration,
                    modifier = Modifier
                        .size(48.dp)
                        .background(AccentRed, CircleShape),
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = stringResource(R.string.stop)
                    )
                }
            } else {
                val canSend = isModelLoaded && text.isNotBlank()
                IconButton(
                    onClick = {
                        if (canSend) {
                            onSendMessage(text)
                            text = ""
                        }
                    },
                    enabled = canSend,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            if (canSend) DarkPrimary else DarkSurfaceVariant,
                            CircleShape
                        ),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = stringResource(R.string.send)
                    )
                }
            }
        }
    }
}
