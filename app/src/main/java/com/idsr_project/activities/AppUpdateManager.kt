package com.idsr_project.activities
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.idsr_project.Model.AppUpdateState
import com.idsr_project.utils.AppUpdateObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

class InAppUpdateManager(context: Context) {
    private val updateManager = AppUpdateManagerFactory.create(context.applicationContext)

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState

    private val installObserver = AppUpdateObserver { state ->
        _updateState.value = state
    }

    suspend fun checkForUpdate(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        _updateState.value = AppUpdateState.Checking

        try {
            val appUpdateInfo = updateManager.appUpdateInfo.await()
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                _updateState.value = AppUpdateState.ReadyToInstall
                return
            }

            when {
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {

                    FirebaseCrashlytics.getInstance().log("Flexible update available.")
                    _updateState.value = AppUpdateState.UpdateAvailable

                    updateManager.registerListener(installObserver)
                    updateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        launcher,
                        AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                    )
                }

                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    updateManager.registerListener(installObserver)
                }

                else -> _updateState.value = AppUpdateState.NoUpdateAvailable
            }
        } catch (e: Exception) {
            FirebaseCrashlytics.getInstance().recordException(e)
            _updateState.value = AppUpdateState.Idle
        }
    }

    fun completeUpdate() {
        updateManager.completeUpdate()
    }

    fun unregister() {
        updateManager.unregisterListener(installObserver)
    }
}