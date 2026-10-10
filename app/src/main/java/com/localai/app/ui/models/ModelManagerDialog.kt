package com.localai.app.ui.models

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localai.app.data.model.CuratedModel
import com.localai.app.data.model.ModelInfo
import com.localai.app.repository.DownloadProgress
import com.localai.app.ui.theme.AccentGreen
import com.localai.app.ui.theme.DarkBackground
import com.localai.app.ui.theme.DarkPrimary
import com.localai.app.ui.theme.DarkSurface
import com.localai.app.ui.theme.DarkSurfaceVariant

@Composable
fun ModelManagerDialog(
    availableModels: List<ModelInfo>,
    curatedModels: List<CuratedModel>,
    activeModel: ModelInfo?,
    downloadProgress: DownloadProgress?,
    onDismiss: () -> Unit,
    onSelectModel: (ModelInfo) -> Unit,
    onDeleteModel: (ModelInfo) -> Unit,
    onImportModel: (Uri, String) -> Unit,
    onDownloadCuratedModel: (CuratedModel) -> Unit,
    onDownloadCustomModel: (String, String) -> Unit,
    onCancelDownload: () -> Unit,
    isModelInstalled: (String) -> Boolean
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var downloadUrl by remember { mutableStateOf("") }
    var downloadFileName by remember { mutableStateOf("custom-model.gguf") }
    var showCustomUrlSection by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val displayName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else null
            } ?: "imported_model.gguf"

            onImportModel(uri, displayName)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Model Management",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Download progress indicator banner if active
                if (downloadProgress != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (downloadProgress.percent >= 0) {
                                        "Downloading… ${(downloadProgress.percent * 100).toInt()}%"
                                    } else {
                                        "Downloading…"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                TextButton(onClick = onCancelDownload) {
                                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            if (downloadProgress.percent >= 0) {
                                LinearProgressIndicator(
                                    progress = { downloadProgress.percent },
                                    modifier = Modifier.fillMaxWidth(),
                                    color = DarkPrimary
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = DarkPrimary
                                )
                            }
                        }
                    }
                }

                // Tab selection: Model Hub vs Installed Models
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = DarkPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = DarkPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Model Hub", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Installed (${availableModels.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedTab == 0) {
                    // TAB 0: MODEL HUB
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(curatedModels) { model ->
                            val installed = isModelInstalled(model.targetFileName)
                            val isCurrentlyLoaded = activeModel?.name == model.targetFileName

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DarkSurfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = model.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${model.publisher} • ${model.formattedSize} • ${model.recommendedRam}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DarkPrimary
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        if (installed) {
                                            OutlinedButton(
                                                onClick = {
                                                    val matched = availableModels.find { it.name == model.targetFileName }
                                                    if (matched != null) onSelectModel(matched)
                                                },
                                                modifier = Modifier.height(34.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = if (isCurrentlyLoaded) AccentGreen else Color.White
                                                )
                                            ) {
                                                Icon(
                                                    Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = if (isCurrentlyLoaded) AccentGreen else Color.LightGray,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    if (isCurrentlyLoaded) "Active" else "Installed",
                                                    fontSize = 12.sp
                                                )
                                            }
                                        } else {
                                            Button(
                                                onClick = { onDownloadCuratedModel(model) },
                                                enabled = downloadProgress == null,
                                                colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                                                modifier = Modifier.height(34.dp)
                                            ) {
                                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Get", fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = model.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Expandable Custom URL input
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { showCustomUrlSection = !showCustomUrlSection },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (showCustomUrlSection) "Hide Custom URL" else "+ Custom GGUF URL",
                                    color = DarkPrimary,
                                    fontSize = 13.sp
                                )
                            }

                            AnimatedVisibility(visible = showCustomUrlSection) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                                    OutlinedTextField(
                                        value = downloadUrl,
                                        onValueChange = { downloadUrl = it },
                                        label = { Text("Direct GGUF URL") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DarkPrimary)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = downloadFileName,
                                        onValueChange = { downloadFileName = it },
                                        label = { Text("Save As File Name") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Button(
                                        onClick = {
                                            if (downloadUrl.isNotBlank()) {
                                                onDownloadCustomModel(downloadUrl, downloadFileName)
                                                showCustomUrlSection = false
                                            }
                                        },
                                        enabled = downloadUrl.isNotBlank() && downloadProgress == null,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Download Custom GGUF")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: INSTALLED MODELS
                    if (availableModels.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No models installed yet.\nVisit the Model Hub tab to download Qwen or Llama.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(availableModels) { model ->
                                val isSelected = activeModel?.filePath == model.filePath
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkPrimary.copy(alpha = 0.15f) else Color.Transparent)
                                        .clickable { onSelectModel(model) }
                                        .padding(horizontal = 8.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Storage,
                                        contentDescription = null,
                                        tint = if (isSelected) DarkPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(
                                            text = model.formattedSize,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = AccentGreen,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteModel(model) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .gguf from Device Storage")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
