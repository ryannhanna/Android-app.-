package com.smartfilemanager.app.ui.screen

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartfilemanager.app.ui.viewmodel.ScanPhase
import com.smartfilemanager.app.ui.viewmodel.ScanViewModel

@Composable
fun RunScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val vm: ScanViewModel = viewModel(factory = ScanViewModel.factory(application))
    val uiState by vm.uiState.collectAsState()

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        vm.onDeleteResult(result.resultCode)
    }

    when (uiState.phase) {
        ScanPhase.PICK_RULE, ScanPhase.SCANNING -> RunRuleContent(
            uiState = uiState,
            onSelectRule = vm::selectRule,
            onSetTargetDirectory = vm::setTargetDirectory,
            onScan = vm::scan,
            modifier = modifier
        )
        ScanPhase.RESULTS -> ScanResultsContent(
            uiState = uiState,
            onToggleSelection = vm::toggleSelection,
            onSelectAll = vm::selectAll,
            onDeselectAll = vm::deselectAll,
            onConfirmDelete = {
                val sender = vm.getDeleteIntentSender()
                if (sender != null) {
                    deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
                }
            },
            onNavigateBack = vm::reset,
            modifier = modifier
        )
        ScanPhase.DONE -> DeletionResultContent(
            result = uiState.deletionResult,
            onDone = vm::reset,
            modifier = modifier
        )
    }
}
