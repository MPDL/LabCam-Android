package com.mpdl.labcam.mvvm.vm.uistate

import com.mpdl.mvvm.base.BaseUiState

data class CameraUiState(
    override var loading: Boolean = false,
    override var showToastMsg: String? = null,
    var loginSuccess: Boolean = false,
    var showFileDirDialog: Boolean = false,
    var getDirError: Boolean = false,
    var checkoutDirPathSuc:Boolean = false,
    val uploadUrlSuc: Boolean = false) : BaseUiState(loading,showToastMsg)