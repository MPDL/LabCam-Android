package com.mpdl.labcam.mvvm.ui.widget;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mpdl.labcam.R;
import com.mpdl.labcam.mvvm.repository.bean.KeeperDirectoryBean;
import com.mpdl.labcam.mvvm.repository.bean.SaveDirectoryBean;
import com.mpdl.mvvm.adapter.BaseRvAdapter;
import com.mpdl.myapplication.base.adapter.BaseRvHolder;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

public class FileSelectorDialog extends AlertDialog {
    private ImageView btnBack;
    private TextView tvTitle;
    private TextView btnSave;
    private RecyclerView rvDir;
    private ProgressBar progressbar;

    private SaveDirectoryBean saveDirectoryBean;
    private DirAdapter mAdapter;
    private List<KeeperDirectoryBean> mData = new ArrayList<>();
    private FileSelectorListener fileSelectorListener;

    public FileSelectorDialog(@NonNull Context context) {
        this(context,0);
    }

    protected FileSelectorDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_file_selector,null);
        btnBack = view.findViewById(R.id.btn_back);
        tvTitle = view.findViewById(R.id.tv_title);
        btnSave = view.findViewById(R.id.btn_save);
        rvDir = view.findViewById(R.id.rv_dir);
        progressbar = view.findViewById(R.id.progressbar);

        rvDir.setLayoutManager(new LinearLayoutManager(context));
        //如果可以确定每个item的高度是固定的，设置这个选项可以提高性能
        rvDir.setHasFixedSize(true);
        DividerItemDecoration itemDecoration = new DividerItemDecoration(context,DividerItemDecoration.VERTICAL);
        itemDecoration.setDrawable(context.getDrawable(R.drawable.shape_item_dir_line));
        rvDir.addItemDecoration(itemDecoration);
        mAdapter = new DirAdapter(mData);
        rvDir.setAdapter(mAdapter);

        btnBack.setOnClickListener(view1 -> {
            if (fileSelectorListener != null){
                if (saveDirectoryBean == null){
                    dismiss();
                    return;
                }
                if ("/".equals(saveDirectoryBean.getPath())){
                    saveDirectoryBean = null;
                    tvTitle.setText("Keeper");
                }else {
                    String path = saveDirectoryBean.getPath();
                    path = path.substring(0,path.lastIndexOf("/"));
                    path = path.substring(0,path.lastIndexOf("/")+1);
                    saveDirectoryBean.setPath(path);
                }

                if (fileSelectorListener != null){
                    fileSelectorListener.onBack(saveDirectoryBean);
                }
            }
        });

        btnSave.setOnClickListener(view1 -> {
            if (fileSelectorListener != null){
                fileSelectorListener.onSave(saveDirectoryBean);
            }
        });

        mAdapter.setOnItemClickListener((view12, viewType, data, position) -> {
            if (data != null){
                if ("repo".equals(data.getType())){
                    if (saveDirectoryBean == null){
                        saveDirectoryBean = new SaveDirectoryBean(data.getId(),data.getName(),"/");
                    }
                }else {
                    if (saveDirectoryBean == null){
                        saveDirectoryBean = new SaveDirectoryBean(data.getId(),data.getName(),"/");
                    }else {
                        String path = saveDirectoryBean.getPath()+data.getName()+"/";
                        saveDirectoryBean.setPath(path);
                    }
                }
            }
            if (fileSelectorListener != null){
                progressbar.setVisibility(View.VISIBLE);
                fileSelectorListener.onItemClick(saveDirectoryBean);
            }
        });
        setView(view);
    }

    @Override
    public void show() {
        saveDirectoryBean = null;
        tvTitle.setText("Keeper");
        if (fileSelectorListener != null){
            fileSelectorListener.onItemClick(null);
        }
        super.show();

        Window window = getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = (int) (window.getWindowManager().getDefaultDisplay().getWidth()*0.85);
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);

    }

    public void setData(List<KeeperDirectoryBean> data) {
        if (progressbar != null){
            progressbar.setVisibility(View.GONE);
        }
        if (data == null)return;
        if (saveDirectoryBean != null){
            tvTitle.setText(saveDirectoryBean.getRepoName()+saveDirectoryBean.getPath());
        }
        mData.clear();
        mData.addAll(data);
        mAdapter.notifyDataSetChanged();
    }

    public void setFileSelectorListener(FileSelectorListener listener){
        fileSelectorListener = listener;
    }

    class DirAdapter extends BaseRvAdapter<KeeperDirectoryBean>{

        public DirAdapter(@NotNull List<? extends KeeperDirectoryBean> info) {
            super(info);
        }

        @NotNull
        @Override
        public BaseRvHolder<KeeperDirectoryBean> getHolder(@NotNull View view, int viewType) {
            return new DirHolder(view);
        }

        @Override
        public int getLayoutId(int viewType) {
            return R.layout.item_directory;
        }

        class DirHolder extends BaseRvHolder<KeeperDirectoryBean>{
            private ImageView ivType;
            private TextView tvDirName;
            public DirHolder(@NotNull View item) {
                super(item);
                ivType = item.findViewById(R.id.iv_type);
                tvDirName = item.findViewById(R.id.tv_dir_name);
            }

            @Override
            public void setData(KeeperDirectoryBean data, int position) {
                if ("repo".equals(data.getType())){
                    ivType.setImageResource(R.mipmap.ic_repo);
                }else {
                    ivType.setImageResource(R.mipmap.ic_dir);
                }
                tvDirName.setText(data.getName());
            }
        }
    }

    public interface FileSelectorListener{
        void onBack(SaveDirectoryBean bean);
        void onSave(SaveDirectoryBean bean);
        void onItemClick(SaveDirectoryBean bean);
    }


}
