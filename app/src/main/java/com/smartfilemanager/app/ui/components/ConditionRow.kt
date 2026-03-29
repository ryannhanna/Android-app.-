package com.smartfilemanager.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.smartfilemanager.app.domain.model.ConditionField
import com.smartfilemanager.app.domain.model.ConditionOperator
import com.smartfilemanager.app.ui.viewmodel.ConditionDraft

@Composable
fun ConditionRow(
    draft: ConditionDraft,
    onFieldChange: (ConditionField) -> Unit,
    onOperatorChange: (ConditionOperator) -> Unit,
    onValueChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            // Row 1: Field picker | Operator picker | Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FieldPickerDropdown(
                    selectedField = draft.field,
                    onFieldSelected = onFieldChange,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OperatorPickerDropdown(
                    selectedField = draft.field,
                    selectedOperator = draft.operator,
                    onOperatorSelected = onOperatorChange,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove condition",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Row 2: Value input + unit label/toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val keyboardType = when (draft.field) {
                    ConditionField.DURATION, ConditionField.AGE, ConditionField.SIZE ->
                        KeyboardType.Number
                    else -> KeyboardType.Text
                }

                OutlinedTextField(
                    value = draft.value,
                    onValueChange = onValueChange,
                    label = { Text("Value") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.weight(1f)
                )

                Spacer(Modifier.width(8.dp))

                when (draft.field) {
                    ConditionField.SIZE -> {
                        // MB / GB toggle buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("MB", "GB").forEach { unit ->
                                FilledTonalButton(
                                    onClick = { onUnitChange(unit) },
                                    colors = if (draft.unit == unit) {
                                        ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else ButtonDefaults.filledTonalButtonColors()
                                ) {
                                    Text(unit)
                                }
                            }
                        }
                    }
                    null -> { /* no unit */ }
                    else -> {
                        // Static unit label (seconds / days)
                        draft.unit?.let { unit ->
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
