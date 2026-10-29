package com.classlog.app.ui.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

object SetupRoutes {
    const val HOME = "setup_home"
    const val BATCHES = "setup_batches"
    const val CATEGORIES = "setup_categories"
    const val TIME_SLOTS = "setup_time_slots"
    const val BACKUP = "setup_backup"
}

@Composable
fun SetupNavHost(
    viewModel: SetupViewModel,
    navController: NavHostController = rememberNavController()
) {
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = SetupRoutes.HOME
    ) {
        composable(SetupRoutes.HOME) {
            SetupHomeScreen(
                uiState = uiState,
                onNavigateBatches = { navController.navigate(SetupRoutes.BATCHES) },
                onNavigateCategories = { navController.navigate(SetupRoutes.CATEGORIES) },
                onNavigateTimeSlots = { navController.navigate(SetupRoutes.TIME_SLOTS) },
                onNavigateBackup = { navController.navigate(SetupRoutes.BACKUP) },
                onSetUse24HourFormat = { viewModel.setUse24HourFormat(it) },
                onSetTeacherName = { viewModel.setTeacherName(it) },
                onSetInstitutionName = { viewModel.setInstitutionName(it) },
                onSaveProfileImage = { ctx, uri -> viewModel.saveProfileImage(ctx, uri) },
                onRemoveProfileImage = { ctx -> viewModel.removeProfileImage(ctx) },
                onSetAppLockEnabled = { viewModel.setAppLockEnabled(it) }
            )
        }

        composable(SetupRoutes.BATCHES) {
            ManageBatchesScreen(
                batches = uiState.batches,
                onBack = { navController.popBackStack() },
                onSaveBatch = { viewModel.saveBatch(it) },
                onUpdateBatch = { viewModel.updateBatch(it) },
                onToggleBatchActive = { viewModel.toggleBatchActive(it) }
            )
        }

        composable(SetupRoutes.CATEGORIES) {
            ManageCategoriesScreen(
                categories = uiState.categories,
                onBack = { navController.popBackStack() },
                onSaveCategory = { name, isTeaching -> viewModel.saveCategory(name, isTeaching) },
                onUpdateCategory = { viewModel.updateCategory(it) },
                onToggleCategoryActive = { viewModel.toggleCategoryActive(it) }
            )
        }

        composable(SetupRoutes.TIME_SLOTS) {
            ManageTimeSlotsScreen(
                timeSlots = uiState.timeSlots,
                use24h = uiState.settings.use24HourFormat,
                onBack = { navController.popBackStack() },
                onSaveTimeSlot = { label, start, end -> viewModel.saveTimeSlot(label, start, end) },
                onUpdateTimeSlot = { viewModel.updateTimeSlot(it) },
                onToggleTimeSlotActive = { viewModel.toggleTimeSlotActive(it) }
            )
        }

        composable(SetupRoutes.BACKUP) {
            BackupRestoreScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
