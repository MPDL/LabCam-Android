package com.mpdl.labcam.mvvm.repository.bean;

import com.mpdl.labcam.R;
import com.mpdl.labcam.treeviewbase.LayoutItemType;

public class KeeperDirItem implements LayoutItemType {

    private String id;
    private String type;
    private String name;
    private String permission;
    private String path;
    private String repoId;
    private String repoName;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRepoId() {
        return repoId;
    }

    public void setRepoId(String repoId) {
        this.repoId = repoId;
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_dir;
    }
}
