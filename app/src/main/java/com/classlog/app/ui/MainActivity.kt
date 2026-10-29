package com.classlog.app.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.classlog.app.ClassLogApplication
import com.classlog.app.ui.home.TodayViewModel
import com.classlog.app.ui.navigation.MainAppScaffold
import com.classlog.app.ui.records.RecordsViewModel
import com.classlog.app.ui.security.BiometricHelper
import com.classlog.app.ui.security.LockOverlay
import com.classlog.app.ui.setup.SetupViewModel
import com.classlog.app.ui.summary.SummaryViewModel
import com.classlog.app.ui.theme.ClassLogTheme

class MainActivity : FragmentActivity() {

    private val setupViewModel: SetupViewModel by viewModels {
        val app = application as ClassLogApplication
        SetupViewModel.provideFactory(app.container.configRepository, app.container.backupRestoreManager)
    }

    private val todayViewModel: TodayViewModel by viewModels {
        val app = application as ClassLogApplication
        TodayViewModel.provideFactory(app.container.sessionRepository, app.container.configRepository)
    }

    private val recordsViewModel: RecordsViewModel by viewModels {
        val app = application as ClassLogApplication
        RecordsViewModel.provideFactory(app.container.sessionRepository, app.container.configRepository)
    }

    private val summaryViewModel: SummaryViewModel by viewModels {
        val app = application as ClassLogApplication
        SummaryViewModel.provideFactory(app.container.sessionRepository, app.container.configRepository)
    }

    // App lock state: if lock is enabled, user must authenticate to unlock
    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ClassLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val setupState by setupViewModel.uiState.collectAsState()
                    val isLockEnabled = setupState.settings.isAppLockEnabled

                    if (isLockEnabled && !isUnlocked) {
                        LockOverlay(
                            onUnlockRequest = {
                                BiometricHelper.showPrompt(
                                    activity = this@MainActivity,
                                    onSuccess = {
                                        isUnlocked = true
                                    }
                                )
                            }
                        )
                    } else {
                        MainAppScaffold(
                            todayViewModel = todayViewModel,
                            recordsViewModel = recordsViewModel,
                            summaryViewModel = summaryViewModel,
                            setupViewModel = setupViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock app when it enters the background if App Lock is enabled
        isUnlocked = false
    }
}
