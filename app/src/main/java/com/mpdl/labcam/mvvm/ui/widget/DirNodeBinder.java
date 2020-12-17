package com.mpdl.labcam.mvvm.ui.widget;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mpdl.labcam.R;
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirItem;
import com.mpdl.labcam.treeviewbase.TreeNode;
import com.mpdl.labcam.treeviewbase.TreeViewBinder;

public class DirNodeBinder extends TreeViewBinder<DirNodeBinder.ViewHolder> {
    private Context mContext;
    @Override
    public ViewHolder provideViewHolder(View itemView) {
        mContext = itemView.getContext();
        return new ViewHolder(itemView);
    }

    @Override
    public void bindView(ViewHolder holder, int position, TreeNode node) {
        KeeperDirItem dirNode = (KeeperDirItem) node.getContent();
        holder.tvDirName.setText(dirNode.getName());
        if (!"dir".equals(dirNode.getType())){
            holder.ivDirImg.setImageResource(R.mipmap.ic_repo);
        }else {
            if (node.isExpand()){
                holder.ivDirImg.setImageResource(R.mipmap.ic_dir_open);
            }else {
                holder.ivDirImg.setImageResource(R.mipmap.ic_dir);
            }
        }
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_dir;
    }

    public static class ViewHolder extends TreeViewBinder.ViewHolder {
        private TextView tvDirName;
        private ImageView ivDirImg;
        private View rootView;

        public ViewHolder(View rootView) {
            super(rootView);
            this.rootView = rootView;
            this.tvDirName = rootView.findViewById(R.id.tv_dir_name);
            this.ivDirImg = rootView.findViewById(R.id.iv_dir_img);
        }
        public TextView getTvDirName() {
            return tvDirName;
        }

        public ImageView getIvDirImg() {
            return ivDirImg;
        }

        public View getRootView() {
            return rootView;
        }
    }
}
