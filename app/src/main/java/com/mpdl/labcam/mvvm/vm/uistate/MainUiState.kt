package com.mpdl.labcam.mvvm.vm.uistate

import com.mpdl.mvvm.base.BaseUiState

data class MainUiState(var referish: Boolean = false,
                       var loadMode: Boolean = false,
                       override var loading: Boolean = false,
                       override var showToastMsg: String? = null) : BaseUiState(loading,showToastMsg)