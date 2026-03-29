package com.smartfilemanager.app.ui.screen

import android.app.Application
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.smartfilemanager.app.data.entity.ConditionEntity
import com.smartfilemanager.app.data.entity.RuleEntity
import com.smartfilemanager.app.data.model.RuleWithConditions
import com.smartfilemanager.app.domain.model.ConditionField
import com.smartfilemanager.app.ui.viewmodel.RulesListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesListScreen(
    onAddRule: () -> Unit,
    onEditRule: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: RulesListViewModel = viewModel(factory = RulesListViewModel.factory(application))
    val rules by vm.rulesFlow.collectAsState(initial = emptyList())

    Box(modifier = modifier.fillMaxSize()) {
        if (rules.isEmpty()) {
            Text(
                text = "No rules yet — tap + to create one",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(rules, key = { it.rule.id }) { ruleWithConditions ->
                    RuleListItem(
                        ruleWithConditions = ruleWithConditions,
                        onDelete = { vm.deleteRule(ruleWithConditions.rule) },
                        onEdit = { onEditRule(ruleWithConditions.rule.id) }
                    )
                    HorizontalDivider()
                }
            }
        }

        FloatingActionButton(
            onClick = onAddRule,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add rule")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RuleListItem(
    ruleWithConditions: RuleWithConditions,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val rule = ruleWithConditions.rule
    val conditions = ruleWithConditions.conditions

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value -> value == SwipeToDismissBoxValue.EndToStart }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                    MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.surface,
                label = "swipe_bg"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onEdit)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = rule.conditionLogic,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (conditions.isNotEmpty()) {
                Text(
                    text = conditionSummary(conditions),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            rule.targetDirectory?.let { dir ->
                Text(
                    text = dir,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun conditionSummary(conditions: List<ConditionEntity>): String {
    return conditions.take(3).joinToString(" · ") { c ->
        val fieldLabel = ConditionField.fromKey(c.field)?.label ?: c.field
        val opSymbol = when (c.operator) { "lt" -> "<"; "gt" -> ">"; "eq" -> "="; "contains" -> "∋"; else -> "∌" }
        val unitAbbrev = when (c.unit) { "seconds" -> "s"; "days" -> "d"; "MB" -> "MB"; "GB" -> "GB"; else -> "" }
        "$fieldLabel $opSymbol ${c.value}$unitAbbrev"
    } + if (conditions.size > 3) " …" else ""
}
