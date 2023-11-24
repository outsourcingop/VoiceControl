package com.optoma.voicecontrol.presenter;

import android.content.Context;
import android.util.Log;

import com.optoma.voicecontrol.LogTextCallback;
import com.optoma.voicecontrol.R;

import io.reactivex.disposables.CompositeDisposable;

public class BasicPresenter {

    protected String TAG = BasicPresenter.class.getSimpleName();

    protected final Context mContext;
    protected final BasicCallback mBasicCallback;

    protected CompositeDisposable mCompositeDisposable = new CompositeDisposable();

    public interface BasicCallback {
        void onLogReceived(String text);

        void onError(String error);
    }

    public BasicPresenter(Context context, BasicCallback basicCallback) {
        mContext = context;
        mBasicCallback = basicCallback;
    }

    public void destroy() {
        if (!mCompositeDisposable.isDisposed()) {
            mCompositeDisposable.dispose();
        }
    }

    final void performError(String errorLog) {
        Log.w(TAG, errorLog);
        mBasicCallback.onLogReceived(errorLog);
        mBasicCallback.onError(errorLog);
    }
}