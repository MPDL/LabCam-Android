package com.mpdl.labcam.mvvm.vm.uistate

import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem
import com.mpdl.labcam.treeviewbase.TreeNode

data class DirTreeViewDialogState(
    var node: TreeNode<KeeperDirItem>? = null,
    var list: List<KeeperDirItem>? = null)