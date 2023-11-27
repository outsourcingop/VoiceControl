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

import com.optoma.voicecontrol.presenter.ChatPresenter;
import com.optoma.voicecontrol.presenter.SpeechRecognizerPresenter;
import com.optoma.voicecontrol.presenter.TextMatcherPresenter;
import com.optoma.voicecontrol.presenter.TranscribePresenter;
import com.optoma.voicecontrol.state.ProcessState;

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
            setState(ProcessState.START_TRANSCRIBE);
            mTranscribePresenter.startContinuousRecognitionAsync(mCurrentLanguage,
                    params.getString(KEY_AUDIO_FILE_PATH));
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

    private final AiServiceCallbackProxy mAiServiceCallback = new AiServiceCallbackProxy();

    private TranscribePresenter mTranscribePresenter;
    private SpeechRecognizerPresenter mSpeechRecognizerPresenter;
    private TextMatcherPresenter mTextMatcherPresenter;
    private ChatPresenter mChatPresenter;

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
        mTranscribePresenter = new TranscribePresenter(this,
                new TranscribePresenter.TranscribeCallback() {
                    @Override
                    public void onLiveCaptionReceived(String recognizedText) {
                        mAiServiceCallback.onLiveCaptionReceived(recognizedText);
                    }

                    @Override
                    public void onAllPartsTranscribed(String transcribeResult) {
                        Log.d(TAG, "onAllPartsTranscribed# transcribedLength=" +
                                transcribeResult.length());
                        setState(ProcessState.END_TRANSCRIBE);
                        setState(ProcessState.START_TEXT_MATCHING);
                        mTextMatcherPresenter.startTextMatching(mCurrentLanguage,
                                transcribeResult);
                    }

                    @Override
                    public void onLogReceived(String text) {
                        mAiServiceCallback.onLogReceived(text);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onTranscribedError# error=" + error);
                        setState(ProcessState.END_TRANSCRIBE);
                        setState(ProcessState.IDLE);
                    }
                });

        mSpeechRecognizerPresenter = new SpeechRecognizerPresenter(this,
                new SpeechRecognizerPresenter.SpeechRecognizerCallback() {
                    @Override
                    public void onLiveCaptionReceived(String recognizedText) {
                        mAiServiceCallback.onLiveCaptionReceived(recognizedText);
                    }

                    @Override
                    public void onSpeechRecognitionCompleted(String recognizedText) {
                        setState(ProcessState.STOP_AUDIO_RECOGNITION);
                        setState(ProcessState.START_TEXT_MATCHING);
                        mTextMatcherPresenter.startTextMatching(mCurrentLanguage, recognizedText);
                    }

                    @Override
                    public void onSpeechRecognitionStoppingAutomatically() {
                        mAiServiceCallback.onSpeechRecognitionStoppingAutomatically();
                    }

                    @Override
                    public void onLogReceived(String text) {
                        mAiServiceCallback.onLogReceived(text);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onSpeechRecognitionError# error=" + error);
                        setState(ProcessState.STOP_AUDIO_RECOGNITION);
                        setState(ProcessState.IDLE);
                    }
                });

        mTextMatcherPresenter = new TextMatcherPresenter(this,
                new TextMatcherPresenter.TextMatcherCallback() {
                    @Override
                    public void onTextMatched(String matchedText, String recognizedText) {
                        Log.d(TAG, "onTextMatched# matchedText=" + matchedText);
                        setState(ProcessState.END_TEXT_MATCHING);
                        if (TextUtils.isEmpty(matchedText) && !TextUtils.isEmpty(recognizedText)) {
                            setState(ProcessState.START_CHAT);
                            mChatPresenter.getChatResponse(mCurrentLanguage, recognizedText);
                        } else {
                            setState(ProcessState.IDLE);
                        }
                    }

                    @Override
                    public void onLogReceived(String text) {
                        mAiServiceCallback.onLogReceived(text);
                    }

                    @Override
                    public void onError(String error) {
                        Log.d(TAG, "onTextMatchedError# error=" + error);
                        setState(ProcessState.END_TEXT_MATCHING);
                        setState(ProcessState.IDLE);
                    }
                });

        mChatPresenter = new ChatPresenter(this, new ChatPresenter.ChatCallback() {
            @Override
            public void onChatRequest(String request) {
                Log.d(TAG, "onChatRequest#");
                String decoratedRequest = getString(R.string.conversation_request_format, request);
                onLogReceived(decoratedRequest);
                mAiServiceCallback.onConversationReceived(decoratedRequest);
            }

            @Override
            public void onChatResponse(String response) {
                Log.d(TAG, "onChatResponse#");
                String decoratedResponse = getString(R.string.conversation_response_format,
                        response);
                onLogReceived(decoratedResponse);
                mAiServiceCallback.onConversationReceived(decoratedResponse);
                setState(ProcessState.END_CHAT);
                setState(ProcessState.IDLE);
            }

            @Override
            public void onLogReceived(String text) {
                mAiServiceCallback.onLogReceived(text);
            }

            @Override
            public void onError(String error) {
                Log.d(TAG, "onChatError# error=" + error);
                setState(ProcessState.END_CHAT);
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
        mChatPresenter.destroy();
    }
}
