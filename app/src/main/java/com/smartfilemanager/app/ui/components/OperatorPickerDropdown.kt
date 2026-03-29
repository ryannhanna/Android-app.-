package com.smartfilemanager.app.ui.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.smartfilemanager.app.domain.model.ConditionField
import com.smartfilemanager.app.domain.model.ConditionOperator
import com.smartfilemanager.app.ui.viewmodel.operatorsFor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OperatorPickerDropdown(
    selectedField: ConditionField,
    selectedOperator: ConditionOperator,
    onOperatorSelected: (ConditionOperator) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val availableOperators = operatorsFor(selectedField)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOperator.label,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Operator") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            availableOperators.forEach { operator ->
                DropdownMenuItem(
                    text = { Text(operator.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onOperatorSelected(operator)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
