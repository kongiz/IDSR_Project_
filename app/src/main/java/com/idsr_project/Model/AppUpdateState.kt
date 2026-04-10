package com.idsr_project.Model

 sealed class AppUpdateState {
     object Idle : AppUpdateState()
     object Checking : AppUpdateState()
     object NoUpdateAvailable : AppUpdateState()
     object UpdateAvailable : AppUpdateState()
     object Downloading : AppUpdateState()
     object ReadyToInstall : AppUpdateState()
     object UpdateComplete : AppUpdateState()
     data class Error(val message: String) : AppUpdateState()
}