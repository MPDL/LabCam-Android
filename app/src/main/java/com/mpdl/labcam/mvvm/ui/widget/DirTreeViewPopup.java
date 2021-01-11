package com.mpdl.labcam.mvvm.ui.widget;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;
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

    public void release(){
        dirTreeViewBuilder.release();
        dirTreeViewBuilder = null;
    }
    @Override
    public void show() {
        if (dirTreeViewBuilder.repoName == null){
            dirTreeViewBuilder.tvDirPath.setText(R.string.keeper);
        }
        if (dirTreeViewBuilder.mDirTreeViewListener != null){
            dirTreeViewBuilder.mDirTreeViewListener.onItemClick(dirTreeViewBuilder.rootNode);
        }
        super.show();
    }

    public DirTreeViewPopup setDirTreeViewListener(DirTreeViewListener listener){
        dirTreeViewBuilder.setDirTreeViewListener(listener);
        return this;
    }

    public void setData(TreeNode node,List<KeeperDirItem> data) {
        if (data == null){
            return;
        }
        if (data.isEmpty()){
//            Toast.makeText(dirTreeViewBuilder.context,"No subdirectories",Toast.LENGTH_SHORT).show();
            return;
        }
        Timber.e("setData TreeNode:"+node +"  data.size:"+data.size());
        //防重复
        if (!node.getChildList().isEmpty()){
            return;
        }
        data = filterData(data);
        for (KeeperDirItem item: data){
            if (node.isRoot()){
                item.setPath("/");
                item.setRepoId(item.getId());
                item.setRepoName(item.getName());
            }else {
                KeeperDirItem parentItem = (KeeperDirItem)node.getContent();
                item.setRepoId(parentItem.getRepoId());
                item.setRepoName(parentItem.getRepoName());
                item.setPath(parentItem.getPath()+item.getName()+"/");
            }
            node.addChild(new TreeNode(item));
        }
        dirTreeViewBuilder.mAdapter.refreshChild(node,dirTreeViewBuilder.curItemHolder);
        if (dirTreeViewBuilder.isRecoveryState){
            dirTreeViewBuilder.recoveryState(node);
        }
    }

    /**
     * 过滤出有权限的目录
     * @param data
     * @return
     */
    private List<KeeperDirItem> filterData(List<KeeperDirItem> data){
        List<KeeperDirItem> filterData = new ArrayList<>();
        //TODO: 确保id唯一，重复repo只取第一次
        List<String> repoIds = new ArrayList<>();
        for (KeeperDirItem bean: data){
            if (DirTreeViewBuilder.isRepo(bean) && bean.isEncrypted()){
                continue;
            }
            if ("rw".equals(bean.getPermission()) && !repoIds.contains(bean.getName())){
                filterData.add(bean);
                repoIds.add(bean.getName());
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
        private TreeNode rootNode;
        private DirTreeViewListener mDirTreeViewListener;
        private String repoName;
        private TreeNode curTreeNode;
        private RecyclerView.ViewHolder curItemHolder;
        private boolean isRecoveryState = false;

        public DirTreeViewBuilder(Context context) {
            this.context = context;
        }

        public void setDirTreeViewListener(DirTreeViewListener mDirTreeViewListener) {
            this.mDirTreeViewListener = mDirTreeViewListener;
        }

        public Builder contentView() {
            return contentView(LayoutInflater.from(context).inflate(R.layout.dialog_dir_tree_view,null));
        }

        public DirTreeViewBuilder isRecoveryState(boolean isRecoveryState){
            this.isRecoveryState = isRecoveryState;
            return this;
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
                        if (mDirTreeViewListener != null){
                            MainActivity.Companion.setCurTreeNodes(curTreeNode.getContents());
                            mDirTreeViewListener.onConfirm(item);
                        }
                    }
                });
                rootNode = TreeNode.root();
                curTreeNode = rootNode;
                rvDir.setLayoutManager(new LinearLayoutManager(context));
                //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
                rvDir.setHasFixedSize(true);
                DividerItemDecoration itemDecoration = new DividerItemDecoration(context,DividerItemDecoration.VERTICAL);
                itemDecoration.setDrawable(context.getDrawable(R.drawable.shape_item_dir_line));
                rvDir.addItemDecoration(itemDecoration);
                mAdapter = new TreeViewAdapter(rootNode.getChildList(), Arrays.asList(new DirNodeBinder()));
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

        public static boolean isRepo(KeeperDirItem item){
            if (item == null){
                return false;
            }
            return "repo".equals(item.getType()) || "srepo".equals(item.getType()) || "grepo".equals(item.getType());
        }

        public void recoveryState(TreeNode treeNode){
            if (MainActivity.Companion.getCurTreeNodes() == null){
                return;
            }
            List<KeeperDirItem> contents = MainActivity.Companion.getCurTreeNodes();
            for (int i = 0; i < treeNode.getChildList().size(); i++){
                TreeNode<KeeperDirItem> node = (TreeNode<KeeperDirItem>) treeNode.getChildList().get(i);
                if (node.getContent() == null){
                    continue;
                }
                for (int j = 0; j < contents.size(); j++){
                    KeeperDirItem content =  contents.get(j);
                    if (content == null){
                        continue;
                    }
                    if (node.getContent().getName().equals(content.getName())){
                        if(mDirTreeViewListener != null){
                            curTreeNode = node;
                            if (isRepo(node.getContent())){
                                repoName = node.getContent().getName();
                                tvDirPath.setText(repoName+"/");
                            }else {
                                tvDirPath.setText(repoName+node.getContent().getPath());
                            }
                            Timber.d("KeeperDirItem: "+content.toString());
                            if (mAdapter != null && mAdapter.getOnTreeNodeListener() != null){
                                mAdapter.getOnTreeNodeListener().onToggle(node.isExpand(),curItemHolder);
                            }
                            mDirTreeViewListener.onItemClick(node);
                            return;
                        }
                    }
                    if (i == treeNode.getChildList().size()-1 && j == contents.size()-1){
                        isRecoveryState = false;
                    }
                }
            }
        }

        public void release(){
            mDirTreeViewListener = null;
            mAdapter = null;
            curTreeNode = null;
            curItemHolder = null;
        }
    }



    public interface DirTreeViewListener{
        void onConfirm(KeeperDirItem item);
        void onItemClick(TreeNode node);
    }

}
