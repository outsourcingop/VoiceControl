package com.optoma.voicecontrol;

import static com.optoma.voicecontrol.AiServiceProxy.KEY_AUDIO_FILE_PATH;
import static com.optoma.voicecontrol.AiServiceProxy.KEY_CALLBACK;
import static com.optoma.voicecontrol.AiServiceProxy.KEY_LANGUAGE;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_VC;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.Nullable;

import com.optoma.voicecontrol.presenter.SpeechRecognizerPresenter;
import com.optoma.voicecontrol.presenter.SummaryPresenter;
import com.optoma.voicecontrol.presenter.TextMatcherPresenter;
import com.optoma.voicecontrol.presenter.TranscribePresenter;
import com.optoma.voicecontrol.state.ProcessState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AiService extends Service {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "AiService" : TAG_VC;

    private final IAiService.Stub mAiService = new IAiService.Stub() {
        @Override
        public void initialize(Bundle params) {
            Log.d(TAG, "AIDL.Stub#initialize params=" + params.size());
            mAiServiceCallback.setProxy((IAiServiceCallback) params.getBinder(KEY_CALLBACK));
        }

        @Override
        public void startAudioProcessing(Bundle params) {
            Log.d(TAG, "AIDL.Stub#startAudioProcessing params=" + params.size());
            mCurrentLanguage = params.getString(KEY_LANGUAGE);
            List<String> audioFilePathList = new ArrayList<>();
            String audioFilePath = params.getString(KEY_AUDIO_FILE_PATH);
            if (audioFilePath != null) {
                audioFilePathList.add(audioFilePath);
            }
            setState(ProcessState.START_TRANSCRIBE);
            mTranscribePresenter.uploadAudioAndTranscribe(audioFilePathList, mCurrentLanguage);
        }

        @Override
        public void startAudioRecognition(Bundle params) {
            Log.d(TAG, "AIDL.Stub#startAudioRecognition params=" + params.size());
            mCurrentLanguage = params.getString(KEY_LANGUAGE);
            setState(ProcessState.START_AUDIO_RECOGNITION);
            mSpeechRecognizerPresenter.startContinuousRecognitionAsync(mCurrentLanguage);
        }

        @Override
        public void stopAudioRecognition(Bundle params) {
            Log.d(TAG, "AIDL.Stub#stopAudioRecognition params=" + params.size());
            mSpeechRecognizerPresenter.stopContinuousRecognitionAsync();
        }

        @Override
        public boolean isAudioRecognizing() {
            return mSpeechRecognizerPresenter.isContinuousRecognition();
        }
    };

    private final LogTextCallback mLogTextCallbackWrapper = new LogTextCallback() {
        @Override
        public void onLogReceived(String text) {
            mAiServiceCallback.onLogReceived(text);
        }
    };

    private final AiServiceCallbackProxy mAiServiceCallback = new AiServiceCallbackProxy();


    private TranscribePresenter mTranscribePresenter;
    private SpeechRecognizerPresenter mSpeechRecognizerPresenter;
    private TextMatcherPresenter mTextMatcherPresenter;
    private SummaryPresenter mSummaryPresenter;

    private String mCurrentLanguage;

    @Override
    public void onCreate() {
        super.onCreate();
        setupPresenter();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mAiService;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        destroyPresenter();
    }

    private void setupPresenter() {
        mTranscribePresenter = new TranscribePresenter(this, mLogTextCallbackWrapper,
                new TranscribePresenter.TranscribeCallback() {
                    @Override
                    public void onTranscribed(String transcribeResult, long timeStamp) {
                        Log.d(TAG, "onTranscribed -> getAndStoreSummary");
                    }

                    @Override
                    public void onAllPartsTranscribed(Map<Integer, String> partNumberToTranscriber,
                            long timeStamp) {
                        Log.d(TAG, "onAllPartsTranscribed -> getAndStoreSummary");
                        setState(ProcessState.END_TRANSCRIBE);
                        setState(ProcessState.START_SUMMARY);
                        mSummaryPresenter.processMultipleConversations(mCurrentLanguage,
                                partNumberToTranscriber, timeStamp);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onTranscribedError# error=" + error);
                        setState(ProcessState.END_TRANSCRIBE);
                        setState(ProcessState.IDLE);
                    }
                });

        mSpeechRecognizerPresenter = new SpeechRecognizerPresenter(this, mLogTextCallbackWrapper,
                new SpeechRecognizerPresenter.SpeechRecognizerCallback() {
                    @Override
                    public void onSpeechRecognitionCompleted(ArrayList<String> texts) {
                        setState(ProcessState.STOP_AUDIO_RECOGNITION);
                        setState(ProcessState.START_TEXT_MATCHING);
                        mTextMatcherPresenter.startTextMatching(texts);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onSpeechRecognitionError# error=" + error);
                        setState(ProcessState.STOP_AUDIO_RECOGNITION);
                        setState(ProcessState.IDLE);
                    }
                });

        mTextMatcherPresenter = new TextMatcherPresenter(this, mLogTextCallbackWrapper,
                new TextMatcherPresenter.TextMatcherCallback() {
                    @Override
                    public void onTextMatched(String matchedText) {
                        Log.d(TAG, "onTextMatched# matchedText=" + matchedText);
                        setState(ProcessState.END_TEXT_MATCHING);
                        if (TextUtils.isEmpty(matchedText)) {
                            // TODO
                            // pass to next presenter (To be removed setState(ProcessState.IDLE))
                            setState(ProcessState.IDLE);
                        } else {
                            setState(ProcessState.IDLE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onTextMatchedError# error=" + error);
                        setState(ProcessState.END_TEXT_MATCHING);
                        setState(ProcessState.IDLE);
                    }
                });

        mSummaryPresenter = new SummaryPresenter(this, mLogTextCallbackWrapper,
                new SummaryPresenter.SummaryCallback() {
                    @Override
                    public void onSummarized() {
                        Log.d(TAG, "onSummarized#");
                        setState(ProcessState.END_SUMMARY);
                        setState(ProcessState.IDLE);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onSummarizedError# error=" + error);
                        setState(ProcessState.END_SUMMARY);
                        setState(ProcessState.IDLE);
                    }
                });
    }

    private void setState(ProcessState state) {
        mAiServiceCallback.onStateChanged(state.name());
    }

    private void destroyPresenter() {
        mTranscribePresenter.destroy();
        mSpeechRecognizerPresenter.destroy();
        mTextMatcherPresenter.destroy();
        mSummaryPresenter.destroy();
    }
}
