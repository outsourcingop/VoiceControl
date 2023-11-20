package com.optoma.voicecontrol.presenter;

import static com.optoma.voicecontrol.BuildConfig.SPEECH_SUBSCRPTION_KEY;

import android.content.Context;
import android.util.Log;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.optoma.voicecontrol.LogTextCallback;
import com.optoma.voicecontrol.R;
import com.optoma.voicecontrol.util.MicrophoneStream;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class SpeechRecognizerPresenter extends BasicPresenter {

    public interface SpeechRecognizerCallback extends ErrorCallback {
        void onSpeechRecognitionCompleted(ArrayList<String> texts);
    }

    private final SpeechRecognizerCallback mSpeechRecognizerCallback;
    private final String mSpeechRegion;
    private final CopyOnWriteArrayList<String> mCopyOnWriteTexts;

    private SpeechConfig mSpeechConfig;
    private MicrophoneStream mMicrophoneStream;
    private AudioConfig mAudioInput;
    private SpeechRecognizer mSpeechRecognizer;

    private boolean mStartContinuousRecognition;


    public SpeechRecognizerPresenter(Context context, LogTextCallback callback,
            SpeechRecognizerCallback speechRecognizerCallback) {
        super(context, callback, speechRecognizerCallback);
        TAG = SpeechRecognizerPresenter.class.getSimpleName();
        mSpeechRecognizerCallback = speechRecognizerCallback;
        mSpeechRegion = context.getResources().getString(R.string.speech_region);
        mCopyOnWriteTexts = new CopyOnWriteArrayList<>();
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
            String s = speechRecognitionResultEventArgs.getResult().getText();
            Log.d(TAG, "Intermediate result received: " + s);
            mLogTextCallback.onLogReceived(s + " ");
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
                            Log.d(TAG, "startContinuousRecognitionAsync# Continuous recognition started.");
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> setContinuousRecognition(true)));
    }

    private void speechRecognitionCompleted() {
        ArrayList<String> texts = new ArrayList<>(mCopyOnWriteTexts);
        // Swipe texts
        mCopyOnWriteTexts.clear();
        setContinuousRecognition(false);
        // Start next steps
        mSpeechRecognizerCallback.onSpeechRecognitionCompleted(texts);
    }

    public void stopContinuousRecognitionAsync() {
        if (!mStartContinuousRecognition || mSpeechRecognizer == null) {
            return;
        }
        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            Log.d(TAG, "stopContinuousRecognitionAsync#");
                            mSpeechRecognizer.stopContinuousRecognitionAsync().get();
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
