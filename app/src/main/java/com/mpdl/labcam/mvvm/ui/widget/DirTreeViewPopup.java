package com.mpdl.labcam.mvvm.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mpdl.labcam.R;
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem;
import com.mpdl.labcam.mvvm.ui.activity.MainActivity;
import com.mpdl.labcam.treeviewbase.TreeNode;
import com.mpdl.labcam.treeviewbase.TreeViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.jessyan.autosize.utils.AutoSizeUtils;
import timber.log.Timber;

public class DirTreeViewPopup extends CustomPopupWindow {
    DirTreeViewBuilder dirTreeViewBuilder;

    protected DirTreeViewPopup(DirTreeViewBuilder builder) {
        super(builder);
        dirTreeViewBuilder = builder;
        dirTreeViewBuilder.btnCancel.setOnClickListener(view ->dismiss());
    }

    public static DirTreeViewBuilder builder(Context context) {
        return new DirTreeViewBuilder(context);
    }

    public TreeNode getCurTreeNode(){
        return dirTreeViewBuilder.curTreeNode;
    }

    public RecyclerView.ViewHolder getCurItemHolder(){
        return dirTreeViewBuilder.curItemHolder;
    }

    @Override
    public void show() {
        if (dirTreeViewBuilder.repoName == null){
            dirTreeViewBuilder.tvDirPath.setText(R.string.keeper);
        }
        if (dirTreeViewBuilder.mDirTreeViewListener != null){
            dirTreeViewBuilder.mDirTreeViewListener.onItemClick(null);
        }
        super.show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        dirTreeViewBuilder.curTreeNode = null;
        dirTreeViewBuilder.repoName = null;
        dirTreeViewBuilder.nodes.clear();
    }

    public DirTreeViewPopup setDirTreeViewListener(DirTreeViewListener listener){
        dirTreeViewBuilder.setDirTreeViewListener(listener);
        return this;
    }

    public void setData(TreeNode node,List<KeeperDirItem> data) {
        if (data == null || data.isEmpty()){
            return;
        }
        Timber.e("setData TreeNode:"+node +"  data.size:"+data.size());
        if (node == null){
            dirTreeViewBuilder.nodes.clear();
        }
        data = filterData(data);
        for (KeeperDirItem item: data){
            if (dirTreeViewBuilder.isRepo(item)){
                item.setPath("/");
                item.setRepoId(item.getId());
                item.setRepoName(item.getName());
                dirTreeViewBuilder.nodes.add(new TreeNode(item));
            }else {
                if (node != null){
                    Timber.e("setData addChild TreeNode");
                    KeeperDirItem parentItem = (KeeperDirItem)node.getContent();
                    item.setRepoId(parentItem.getRepoId());
                    item.setRepoName(parentItem.getRepoName());
                    item.setPath(parentItem.getPath()+item.getName()+"/");
                    node.addChild(new TreeNode(item));
                }
            }
        }
        if (node == null){
            dirTreeViewBuilder.mAdapter.refresh(dirTreeViewBuilder.nodes);
        }else {
            dirTreeViewBuilder.mAdapter.refreshChild(node,dirTreeViewBuilder.curItemHolder);
        }
    }

    /**
     * 过滤出有权限的目录
     * @param data
     * @return
     */
    private List<KeeperDirItem> filterData(List<KeeperDirItem> data){
        List<KeeperDirItem> filterData = new ArrayList<>();
        for (KeeperDirItem bean: data){
            if ("rw".equals(bean.getPermission())){
                filterData.add(bean);
            }
        }
        return filterData;
    }



    public static class DirTreeViewBuilder extends Builder{
        private Context context;
        private TextView btnCancel;
        private TextView btnConfirm;
        private TextView tvDirPath;
        private RecyclerView rvDir;
        private TreeViewAdapter mAdapter;
        private List<TreeNode> nodes = new ArrayList<>();
        private DirTreeViewListener mDirTreeViewListener;
        private String repoName;
        private TreeNode curTreeNode;
        private RecyclerView.ViewHolder curItemHolder;

        public DirTreeViewBuilder(Context context) {
            this.context = context;
        }

        public void setDirTreeViewListener(DirTreeViewListener mDirTreeViewListener) {
            this.mDirTreeViewListener = mDirTreeViewListener;
        }

        public Builder contentView() {
            return contentView(LayoutInflater.from(context).inflate(R.layout.dialog_dir_tree_view,null));
        }

        public Builder customListener() {
            return customListener(view -> {
                btnCancel = view.findViewById(R.id.btn_cancel);
                btnConfirm = view.findViewById(R.id.btn_confirm);
                tvDirPath = view.findViewById(R.id.tv_dir_path);
                rvDir = view.findViewById(R.id.rv_tree_dir);

                btnConfirm.setOnClickListener(view1->{
                    if (mDirTreeViewListener != null){
                        KeeperDirItem item = null;
                        if (curTreeNode != null){
                            item = (KeeperDirItem)curTreeNode.getContent();
                        }
                        MainActivity.Companion.setCurDirItem(item);
                        if (mDirTreeViewListener != null){
                            mDirTreeViewListener.onConfirm(item);
                        }
                    }
                });

                rvDir.setLayoutManager(new LinearLayoutManager(context));
                //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
                rvDir.setHasFixedSize(true);
                DividerItemDecoration itemDecoration = new DividerItemDecoration(context,DividerItemDecoration.VERTICAL);
                itemDecoration.setDrawable(context.getDrawable(R.drawable.shape_item_dir_line));
                rvDir.addItemDecoration(itemDecoration);
                mAdapter = new TreeViewAdapter(nodes, Arrays.asList(new DirNodeBinder()));
                mAdapter.setPadding(AutoSizeUtils.dp2px(context,15));
                mAdapter.setOnTreeNodeListener(new TreeViewAdapter.OnTreeNodeListener() {
                    @Override
                    public boolean onClick(TreeNode node, RecyclerView.ViewHolder holder) {
                        curTreeNode = node;
                        curItemHolder = holder;
                        KeeperDirItem item = (KeeperDirItem) node.getContent();
                        if (isRepo(item)){
                            repoName = item.getName();
                            tvDirPath.setText(repoName+"/");
                        }else {
                            tvDirPath.setText(repoName+item.getPath());
                        }
                        if (!node.isExpand()){
                            mAdapter.collapseBrotherNode(node);
                        }
                        if (!node.isLeaf()) {
                            //Update and toggle the node.
                            onToggle(!node.isExpand(), holder);
                        }
                        if (mDirTreeViewListener != null){
                            mDirTreeViewListener.onItemClick(node);
                        }

                        return false;
                    }

                    @Override
                    public void onToggle(boolean isExpand, RecyclerView.ViewHolder holder) {
                        if (curTreeNode != null){
                            if (!isRepo((KeeperDirItem) curTreeNode.getContent())){
                                rvDir.post(() -> {
                                    DirNodeBinder.ViewHolder dirViewHolder = (DirNodeBinder.ViewHolder) holder;
                                    if (dirViewHolder != null){
                                        if (isExpand){
                                            dirViewHolder.getIvDirImg().setImageResource(R.mipmap.ic_dir_open);
                                        }else {
                                            dirViewHolder.getIvDirImg().setImageResource(R.mipmap.ic_dir);
                                        }
                                    }
                                    mAdapter.notifyDataSetChanged();
                                });
                            }
                        }

                    }
                });
                rvDir.setAdapter(mAdapter);
            });
        }

        @Override
        public DirTreeViewPopup build() {
            contentView();
            customListener();
            if (contentView == null)
                throw new IllegalStateException("ContentView is required");
            if (listener == null)
                throw new IllegalStateException("CustomPopupWindowListener is required");

            return new DirTreeViewPopup(this);
        }

        public boolean isRepo(KeeperDirItem item){
            if (item == null){
                return false;
            }
            return "repo".equals(item.getType());
        }
    }



    public interface DirTreeViewListener{
        void onConfirm(KeeperDirItem item);
        void onItemClick(TreeNode node);
    }

}
