package com.smartfilemanager.app.ui.screen

import android.app.Application
import android.net.Uri
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartfilemanager.app.ui.components.ConditionRow
import com.smartfilemanager.app.ui.viewmodel.RuleBuilderViewModel

private fun treeUriToPath(uri: Uri): String {
    val docId = DocumentsContract.getTreeDocumentId(uri)
    return if (docId.startsWith("primary:")) {
        "/storage/emulated/0/${docId.removePrefix("primary:")}"
    } else {
        val parts = docId.split(":", limit = 2)
        "/storage/${parts[0]}/${parts.getOrElse(1) { "" }}"
    }
}

@Composable
fun RuleBuilderScreen(
    ruleId: Int?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: RuleBuilderViewModel = viewModel(
        // Key by ruleId so a new VM instance is created per rule
        key = "rule_builder_${ruleId ?: "new"}",
        factory = RuleBuilderViewModel.factory(application, ruleId)
    )
    val uiState by vm.uiState.collectAsState()

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            vm.updateTargetDirectory(treeUriToPath(uri))
        }
    }

    // Navigate back automatically after a successful save
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            vm.clearSaveSuccess()
            onNavigateBack()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // Screen header bar: back button + title + Save action
        Surface(tonalElevation = 2.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
                Text(
                    text = if (ruleId == null) "New Rule" else "Edit Rule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.padding(horizontal = 12.dp))
                } else {
                    TextButton(
                        onClick = { vm.saveRule() },
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        HorizontalDivider()

        // Scrollable form content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Rule name ---
            Text("Rule name", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { vm.updateName(it) },
                placeholder = { Text("e.g. Short Clips") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            // --- Conditions ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Match", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.padding(horizontal = 4.dp))

                // AND / OR toggle
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("AND", "OR").forEach { logic ->
                        FilledTonalButton(
                            onClick = { vm.updateLogic(logic) },
                            colors = if (uiState.conditionLogic == logic) {
                                ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            } else ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(logic)
                        }
                    }
                }

                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("of the following", style = MaterialTheme.typography.bodyMedium)
            }

            // Condition rows
            uiState.conditions.forEach { draft ->
                ConditionRow(
                    draft = draft,
                    onFieldChange = { vm.updateConditionField(draft.id, it) },
                    onOperatorChange = { vm.updateConditionOperator(draft.id, it) },
                    onValueChange = { vm.updateConditionValue(draft.id, it) },
                    onUnitChange = { vm.updateConditionUnit(draft.id, it) },
                    onDelete = { vm.removeCondition(draft.id) }
                )
            }

            // Add condition button
            TextButton(
                onClick = { vm.addCondition() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add condition")
            }

            HorizontalDivider()

            // --- Target folder (optional) ---
            Text("Scan folder (optional)", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = uiState.targetDirectory,
                onValueChange = { vm.updateTargetDirectory(it) },
                placeholder = { Text("e.g. /storage/emulated/0/DCIM") },
                singleLine = true,
                supportingText = { Text("Leave blank to scan all storage") },
                trailingIcon = {
                    IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Icon(
                            imageVector = Icons.Filled.FolderOpen,
                            contentDescription = "Browse folders"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // --- Inline error ---
            uiState.error?.let { errorMsg ->
                Text(
                    text = errorMsg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(8.dp))

            // --- Save button ---
            Button(
                onClick = { vm.saveRule() },
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Rule")
            }
        }
    }
}
