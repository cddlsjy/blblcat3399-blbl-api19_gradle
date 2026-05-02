package blbl.cat3399.feature.settings

sealed interface TestUpdateCheckState {
    data object Idle : TestUpdateCheckState

    data object Checking : TestUpdateCheckState

    data class Latest(val latestVersion: String) : TestUpdateCheckState

    data class UpdateAvailable(val latestVersion: String) : TestUpdateCheckState

    data class Error(val message: String) : TestUpdateCheckState
}

class SettingsState {
    var currentSectionIndex: Int = -1

    var lastFocusedLeftIndex: Int = 0
    var lastFocusedRightId: SettingId? = null
    var pendingRestoreRightId: SettingId? = null
    var pendingRestoreLeftIndex: Int? = null
    var pendingRestoreBack: Boolean = false
    var focusRequestToken: Int = 0

    var cacheSizeBytes: Long? = null

    var testUpdateCheckState: TestUpdateCheckState = TestUpdateCheckState.Idle
    var testUpdateCheckedAtMs: Long = -1L
}

