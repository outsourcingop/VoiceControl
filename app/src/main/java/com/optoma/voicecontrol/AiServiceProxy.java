package com.optoma.voicecontrol;

import static com.optoma.voicecontrol.util.DebugConfig.TAG_VC;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.Nullable;

public class AiServiceProxy extends IAiService.Stub {

    private static final String TAG = TAG_WITH_CLASS_NAME ? AiServiceProxy.class.getSimpleName() : TAG_VC;

    public final static String KEY_CALLBACK = "callback";

    public final static String KEY_LANGUAGE = "language";

    public final static String KEY_AUDIO_FILE_PATH = "path";

    private final IAiService mDefaultAiServiceProxy = new DefaultAiServiceProxy();

    private IAiService mAiService = mDefaultAiServiceProxy;


    public void setProxy(@Nullable IAiService aiService) {
        if (aiService != null) {
            mAiService = aiService;
        } else {
            mAiService = mDefaultAiServiceProxy;
        }
    }

    @Override
    public void initialize(Bundle params) {
        try {
            mAiService.initialize(params);
        } catch (RemoteException e) {
            Log.w(TAG, "run initialize() but " + e);
        }
    }

    @Override
    public void startAudioProcessing(Bundle params) {
        try {
            mAiService.startAudioProcessing(params);
        } catch (RemoteException e) {
            Log.w(TAG, "run startAudioProcessing() but " + e);
        }
    }

    @Override
    public void startAudioRecognition(Bundle params) {
        try {
            mAiService.startAudioRecognition(params);
        } catch (RemoteException e) {
            Log.w(TAG, "run startAudioRecognition() but " + e);
        }
    }

    @Override
    public void stopAudioRecognition(Bundle params) {
        try {
            mAiService.stopAudioRecognition(params);
        } catch (RemoteException e) {
            Log.w(TAG, "run stopAudioRecognition() but " + e);
        }
    }

    @Override
    public boolean isAudioRecognizing() {
        try {
            return mAiService.isAudioRecognizing();
        } catch (RemoteException e) {
            Log.w(TAG, "run isAudioRecognizing() but " + e);
        }
        return false;
    }

    private static class DefaultAiServiceProxy extends IAiService.Stub {

        @Override
        public void initialize(Bundle params) {
        }

        @Override
        public void startAudioProcessing(Bundle params) {
        }

        public void startAudioRecognition(Bundle params) {
        }

        @Override
        public void stopAudioRecognition(Bundle params) {
        }

        @Override
        public boolean isAudioRecognizing() {
            return false;
        }
    }

}
