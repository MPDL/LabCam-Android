package com.mpdl.labcam.mvvm.ui.widget;

import android.content.Context;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.mpdl.labcam.R;

import me.jessyan.autosize.utils.AutoSizeUtils;

public class TipsDialog extends AlertDialog {
    private Context context;
    protected TipsDialog(@NonNull Context context) {
        this(context,0);
    }

    protected TipsDialog(@NonNull Context context, int themeResId) {
        super(context, themeResId);
        this.context = context;
    }

    @Override
    public void show() {
        super.show();
        Window window = getWindow();
        window.setBackgroundDrawableResource(android.R.color.transparent);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.width = AutoSizeUtils.dp2px(context,274);//宽高可设置具体大小;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        getWindow().setAttributes(lp);
    }

    public static class TipsBuilder extends Builder{

        private Context mContext;
        private int mThemeResId;
        private CharSequence mTitle;
        private CharSequence mMessage;
        private CharSequence mNegativeButtonText;
        private CharSequence mPositiveButtonText;
        private OnClickListener mNegativeButtonListener;
        private OnClickListener mPositiveButtonListener;

        public TipsBuilder(@NonNull Context context) {
            this(context,R.style.Theme_AppCompat_Dialog_Alert);
        }

        public TipsBuilder setTitle(@StringRes int titleId) {
            mTitle = mContext.getText(titleId);
            return this;
        }

        public TipsBuilder setTitle(@Nullable CharSequence title) {
            mTitle = title;
            return this;
        }

        public TipsBuilder setMessage(@StringRes int messageId) {
            mMessage = mContext.getText(messageId);
            return this;
        }

        public TipsBuilder setMessage(@Nullable CharSequence message) {
            mMessage = message;
            return this;
        }

        public TipsBuilder setNegativeButton(@StringRes int textId, final OnClickListener listener){
            mNegativeButtonText = mContext.getText(textId);
            mNegativeButtonListener = listener;
            return this;
        }

        public TipsBuilder setNegativeButton(CharSequence text, final OnClickListener listener){
            mNegativeButtonText = text;
            mNegativeButtonListener = listener;
            return this;
        }

        public TipsBuilder setPositiveButton(@StringRes int textId, final OnClickListener listener) {
            mPositiveButtonText = mContext.getText(textId);
            mPositiveButtonListener = listener;
            return this;
        }

        public TipsBuilder setPositiveButton(String text, final OnClickListener listener) {
            mPositiveButtonText = text;
            mPositiveButtonListener = listener;
            return this;
        }

        public TipsBuilder(@NonNull Context context, int themeResId) {
            super(context, themeResId);
            mContext = context;
            mThemeResId = themeResId;
        }

        @NonNull
        @Override
        public TipsDialog create() {
            TipsDialog dialog = new TipsDialog(mContext,mThemeResId);
            View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_tips,null);
            TextView tvTitle = view.findViewById(R.id.tv_title);
            TextView tvMessage = view.findViewById(R.id.tv_message);
            TextView btnNegative = view.findViewById(R.id.btn_negative);
            TextView btnPositive = view.findViewById(R.id.btn_positive);
            View lineBtn = view.findViewById(R.id.line_btn);
            dialog.setView(view);

            tvTitle.setText(mTitle);
            tvMessage.setText(mMessage);
            if (TextUtils.isEmpty(mNegativeButtonText)){
                btnNegative.setVisibility(View.GONE);
                lineBtn.setVisibility(View.GONE);
            }else {
                btnNegative.setText(mNegativeButtonText);
                btnNegative.setOnClickListener(view1 -> {
                    if (mNegativeButtonListener != null){
                        mNegativeButtonListener.onClick(dialog,0);
                    }else {
                        dialog.dismiss();
                    }
                });
            }

            if (TextUtils.isEmpty(mPositiveButtonText)){
                btnPositive.setVisibility(View.GONE);
                lineBtn.setVisibility(View.GONE);
            }else {
                btnPositive.setText(mPositiveButtonText);
                btnPositive.setOnClickListener(view1 -> {
                    if (mPositiveButtonListener != null){
                        mPositiveButtonListener.onClick(dialog,1);
                    }else {
                        dialog.dismiss();
                    }
                });
            }
            return dialog;
        }
    }
}
