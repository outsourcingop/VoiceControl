package com.optoma.voicecontrol;

import static com.optoma.voicecontrol.util.DebugConfig.TAG_VC;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

public class AiServiceCallbackProxy extends IAiServiceCallback.Stub {

    private static final String TAG = TAG_WITH_CLASS_NAME ? AiServiceCallbackProxy.class.getSimpleName() : TAG_VC;

    private IAiServiceCallback mAiServiceCallback;


    public void setProxy(@Nullable IAiServiceCallback aiService) {
        if (aiService != null) {
            mAiServiceCallback = aiService;
        } else {
            mAiServiceCallback = new DefaultAiServiceCallbackProxy();
        }
    }

    @Override
    public void onStateChanged(String state) {
        try {
            mAiServiceCallback.onStateChanged(state);
        } catch (RemoteException e) {
            Log.w(TAG, "run startAudioProcessing() but " + e);
        }
    }

    @Override
    public void onLogReceived(String text) {
        try {
            mAiServiceCallback.onLogReceived(text);
        } catch (RemoteException e) {
            Log.w(TAG, "run onLogReceived() but " + e);
        }
    }

    @Override
    public void onConversationReceived(String text) {
        try {
            mAiServiceCallback.onConversationReceived(text);
        } catch (RemoteException e) {
            Log.w(TAG, "run onConversationReceived() but " + e);
        }
    }

    private static class DefaultAiServiceCallbackProxy extends IAiServiceCallback.Stub {
        @Override
        public void onStateChanged(String state) {
        }

        @Override
        public void onLogReceived(String text) {
        }

        @Override
        public void onConversationReceived(String text) {
        }
    }
}
