package com.mpdl.labcam.mvvm.vm.uistate

import com.mpdl.mvvm.base.BaseUiState

data class LoginUiState(
    override var loading: Boolean = false,
    override var showToastMsg: String? = null,
    var loginSuccess: Boolean = false,
    var showFileSelectorDialog: Boolean = false) : BaseUiState(loading,showToastMsg)