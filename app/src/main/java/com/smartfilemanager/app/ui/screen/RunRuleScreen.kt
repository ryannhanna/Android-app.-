package com.smartfilemanager.app.ui.screen

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.ui.viewmodel.ScanPhase
import com.smartfilemanager.app.ui.viewmodel.ScanUiState

@Composable
fun RunRuleContent(
    uiState: ScanUiState,
    onSelectRule: (RuleEntity) -> Unit,
    onSetTargetDirectory: (String?) -> Unit,
    onScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.phase == ScanPhase.SCANNING -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Scanning…", style = MaterialTheme.typography.bodyMedium)
                }
            }
            uiState.savedRules.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No rules yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Create a rule in the Rules tab, then come back to run it.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Run a Rule",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    HorizontalDivider()

                    // Rule picker
                    Text("Select rule", style = MaterialTheme.typography.labelLarge)
                    RulePickerDropdown(
                        rules = uiState.savedRules,
                        selectedRule = uiState.selectedRule,
                        onRuleSelected = onSelectRule,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider()

                    // Folder selection
                    Text("Scan folder", style = MaterialTheme.typography.labelLarge)

                    // All storage radio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.targetDirectory == null,
                                onClick = { onSetTargetDirectory(null) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.targetDirectory == null,
                            onClick = { onSetTargetDirectory(null) }
                        )
                        Text(
                            text = "All storage",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Specific folder radio
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = uiState.targetDirectory != null,
                                onClick = {
                                    onSetTargetDirectory(
                                        uiState.selectedRule?.targetDirectory ?: ""
                                    )
                                },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.targetDirectory != null,
                            onClick = {
                                onSetTargetDirectory(
                                    uiState.selectedRule?.targetDirectory ?: ""
                                )
                            }
                        )
                        Text(
                            text = "Specific folder",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (uiState.targetDirectory != null) {
                        OutlinedTextField(
                            value = uiState.targetDirectory,
                            onValueChange = { onSetTargetDirectory(it) },
                            placeholder = { Text("e.g. /DCIM/Camera") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    uiState.error?.let { err ->
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = onScan,
                        enabled = uiState.selectedRule != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan Now")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RulePickerDropdown(
    rules: List<RuleEntity>,
    selectedRule: RuleEntity?,
    onRuleSelected: (RuleEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedRule?.name ?: "",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Rule") },
            placeholder = { Text("Select a rule…") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            rules.forEach { rule ->
                DropdownMenuItem(
                    text = { Text(rule.name) },
                    onClick = {
                        onRuleSelected(rule)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
