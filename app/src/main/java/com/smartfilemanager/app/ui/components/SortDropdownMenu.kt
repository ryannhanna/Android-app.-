package com.smartfilemanager.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.smartfilemanager.app.ui.viewmodel.SortOrder

@Composable
fun SortDropdownMenu(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }, modifier = modifier) {
        Icon(Icons.Filled.Sort, contentDescription = "Sort")
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        SortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = { Text(sortOrder.label) },
                onClick = {
                    onSortSelected(sortOrder)
                    expanded = false
                },
                leadingIcon = if (sortOrder == currentSort) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else null
            )
        }
    }
}
