package com.optoma.voicecontrol.presenter;

import static com.optoma.voicecontrol.BuildConfig.SPEECH_SUBSCRPTION_KEY;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.optoma.voicecontrol.R;
import com.optoma.voicecontrol.util.MicrophoneStream;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SpeechRecognizerPresenter extends BasicPresenter {

    private static final int MESSAGE_CONTINUOUS_RECOGNITION_TIMEOUT = 0;

    public interface SpeechRecognizerCallback extends BasicCallback {
        void onLiveCaptionReceived(String recognizedText);

        void onSpeechRecognitionCompleted(String recognizedText);

        void onSpeechRecognitionStoppingAutomatically();
    }

    private final SpeechRecognizerCallback mSpeechRecognizerCallback;
    private final String mSpeechRegion;
    private final CopyOnWriteArrayList<String> mCopyOnWriteTexts;
    private final int mAutomaticallyStopSpeechRecognitionTimeout;
    private final Handler mHandler;

    private SpeechConfig mSpeechConfig;
    private MicrophoneStream mMicrophoneStream;
    private AudioConfig mAudioInput;
    private SpeechRecognizer mSpeechRecognizer;

    private boolean mStartContinuousRecognition;


    public SpeechRecognizerPresenter(Context context,
            SpeechRecognizerCallback speechRecognizerCallback) {
        super(context, speechRecognizerCallback);
        TAG = SpeechRecognizerPresenter.class.getSimpleName();
        mSpeechRecognizerCallback = speechRecognizerCallback;
        mSpeechRegion = context.getResources().getString(R.string.speech_region);
        mCopyOnWriteTexts = new CopyOnWriteArrayList<>();
        mAutomaticallyStopSpeechRecognitionTimeout =
                context.getResources().getInteger(R.integer.automatically_stop_speech_recognition);
        mHandler = new Handler(Looper.getMainLooper(), message -> {
            if (message.what == MESSAGE_CONTINUOUS_RECOGNITION_TIMEOUT) {
                mSpeechRecognizerCallback.onSpeechRecognitionStoppingAutomatically();
                return true;
            }
            return false;
        });
    }

    public void startContinuousRecognitionAsync(String currentLanguage) {
        Log.d(TAG, "startContinuousRecognitionAsync# currentLanguage=" + currentLanguage);
        mCopyOnWriteTexts.clear();

        mSpeechConfig = SpeechConfig.fromSubscription(SPEECH_SUBSCRPTION_KEY, mSpeechRegion);
        mSpeechConfig.setSpeechRecognitionLanguage(currentLanguage);
        mMicrophoneStream = new MicrophoneStream();
        mAudioInput = AudioConfig.fromStreamInput(mMicrophoneStream);
        mSpeechRecognizer = new SpeechRecognizer(mSpeechConfig, mAudioInput);

        mSpeechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
            startContinuousRecognitionStoppingTimer();
            String s = speechRecognitionResultEventArgs.getResult().getText();
            Log.d(TAG, "Intermediate result received: " + s);
            mSpeechRecognizerCallback.onLiveCaptionReceived(s);
        });

        mSpeechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            mCopyOnWriteTexts.add(s);
            Log.d(TAG, "Final result received: " + s);
        });

        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            Log.d(TAG, "startContinuousRecognitionAsync#");
                            mSpeechRecognizer.startContinuousRecognitionAsync().get();
                            startContinuousRecognitionStoppingTimer();
                            Log.d(TAG, "startContinuousRecognitionAsync# Continuous recognition started.");
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> setContinuousRecognition(true)));
    }

    private void startContinuousRecognitionStoppingTimer() {
        Log.d(TAG, "startContinuousRecognitionStoppingTimer#");
        Message message = Message.obtain(mHandler, MESSAGE_CONTINUOUS_RECOGNITION_TIMEOUT);
        mHandler.removeMessages(message.what);
        mHandler.sendMessageDelayed(message, mAutomaticallyStopSpeechRecognitionTimeout);
    }

    private void stopContinuousRecognitionStoppingTimer() {
        Message message = Message.obtain(mHandler, MESSAGE_CONTINUOUS_RECOGNITION_TIMEOUT);
        mHandler.removeMessages(message.what);
    }

    private void speechRecognitionCompleted() {
        ArrayList<String> recognizedTexts = new ArrayList<>(mCopyOnWriteTexts);
        // Swipe texts
        mCopyOnWriteTexts.clear();
        setContinuousRecognition(false);
        // Start next steps
        StringBuilder serializedRecognizedText = new StringBuilder();
        for (String recognizedText : recognizedTexts) {
            if (TextUtils.isEmpty(recognizedText)) {
                continue;
            }
            serializedRecognizedText.append(recognizedText).append(" ");
        }
        mSpeechRecognizerCallback.onSpeechRecognitionCompleted(serializedRecognizedText.toString());
    }

    public void stopContinuousRecognitionAsync() {
        if (!mStartContinuousRecognition || mSpeechRecognizer == null) {
            return;
        }
        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            Log.d(TAG, "stopContinuousRecognitionAsync#");
                            mSpeechRecognizer.stopContinuousRecognitionAsync().get();
                            stopContinuousRecognitionStoppingTimer();
                            Log.d(TAG, "stopContinuousRecognitionAsync# Continuous recognition stopped.");
                            mMicrophoneStream.close();
                            mAudioInput.close();
                            mSpeechConfig.close();
                            mSpeechRecognizer.close();
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::speechRecognitionCompleted)
        );
    }

    private void setContinuousRecognition(boolean startContinuousRecognition) {
        mStartContinuousRecognition = startContinuousRecognition;
    }

    public boolean isContinuousRecognition() {
        return mStartContinuousRecognition;
    }

    @Override
    public void destroy() {
        super.destroy();
        stopContinuousRecognitionAsync();
    }
}
