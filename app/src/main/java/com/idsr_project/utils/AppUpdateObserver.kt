package com.idsr_project.utils

import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.idsr_project.Model.AppUpdateState

class AppUpdateObserver(
    private val onStateChanged: (AppUpdateState) -> Unit
) : InstallStateUpdatedListener {

    override fun onStateUpdate(state: InstallState) {
        when (state.installStatus()) {

            InstallStatus.DOWNLOADING -> {
                onStateChanged(AppUpdateState.Downloading)
            }

            InstallStatus.DOWNLOADED -> {
                onStateChanged(AppUpdateState.ReadyToInstall)
            }

            InstallStatus.FAILED -> {
                onStateChanged(AppUpdateState.Error("Update download failed. Please try again."))
            }

            InstallStatus.CANCELED -> {
                onStateChanged(AppUpdateState.Idle)
            }

            else -> {
                // PENDING, INSTALLING, INSTALLED, UNKNOWN — no UI action needed
            }
        }
    }
}