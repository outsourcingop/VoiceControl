package com.optoma.voicecontrol.presenter;

import static com.optoma.voicecontrol.BuildConfig.SPEECH_SUBSCRPTION_KEY;
import static com.optoma.voicecontrol.util.FileUtil.createNewAudioFilePath;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.optoma.voicecontrol.R;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class TranscribePresenter extends BasicPresenter {
    private final TranscribeCallback mTranscribeCallback;

    public TranscribePresenter(Context context, TranscribeCallback transcribeCallback) {
        super(context, transcribeCallback);
        TAG = TranscribePresenter.class.getSimpleName();
        mTranscribeCallback = transcribeCallback;
        mSpeechRegion = context.getResources().getString(R.string.speech_region);
        mCopyOnWriteTexts = new CopyOnWriteArrayList<>();
    }

    public interface TranscribeCallback extends BasicCallback {
        void onLiveCaptionReceived(String recognizedText);

        void onAllPartsTranscribed(String transcribeResult);
    }

    private final String mSpeechRegion;
    private final CopyOnWriteArrayList<String> mCopyOnWriteTexts;

    public void startContinuousRecognitionAsync(String currentLanguage, String absolutePathList) {
        Log.d(TAG, "startContinuousRecognitionAsync# currentLanguage=" + currentLanguage);

        if (!isWavFile(absolutePathList)) {
            String newFileAbsolutePath = createNewAudioFilePath(absolutePathList, 0);

            String command =
                    " -i " + absolutePathList +
                            " -ar 16000 -ac 1 -c:a pcm_s16le "
                            + newFileAbsolutePath;

            FFmpegKit.executeAsync(command, session -> {
                SessionState state = session.getState();
                ReturnCode returnCode = session.getReturnCode();
                // CALLED WHEN SESSION IS EXECUTED
                Log.d(TAG, String.format(
                        "FFmpeg process the filePartNumber=%d exited with state %s and rc %s.%s",
                        0, state, returnCode,
                        session.getFailStackTrace()));
                startRecognize(currentLanguage, newFileAbsolutePath);
            });
        } else {
            startRecognize(currentLanguage, absolutePathList);
        }
    }

    private void startRecognize(String currentLanguage, String wavFileAbsPath) {
        SpeechConfig speechConfig = SpeechConfig.fromSubscription(SPEECH_SUBSCRPTION_KEY,
                mSpeechRegion);
        speechConfig.setSpeechRecognitionLanguage(currentLanguage);
        AudioConfig audioInput = AudioConfig.fromWavFileInput(wavFileAbsPath);
        SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioInput);

        speechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            Log.d(TAG, "Intermediate result received: " + s);
            mTranscribeCallback.onLiveCaptionReceived(s);
        });
        speechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            mCopyOnWriteTexts.add(s);
            Log.d(TAG, "Final result received: " + s);
            mBasicCallback.onLogReceived("Final result: " + s);
        });
        speechRecognizer.sessionStopped.addEventListener((o, speechRecognitionResultEventArgs) -> {
            Log.d(TAG, "sessionStopped !!");
            mTranscribeCallback.onAllPartsTranscribed(serializedRecognizedText());
            audioInput.close();
            speechConfig.close();
            speechRecognizer.close();
        });
        speechRecognizer.recognizeOnceAsync();
    }

    private String serializedRecognizedText() {
        ArrayList<String> recognizedTexts = new ArrayList<>(mCopyOnWriteTexts);
        // Swipe texts
        mCopyOnWriteTexts.clear();
        // Start next steps
        StringBuilder serializedRecognizedText = new StringBuilder();
        for (String recognizedText : recognizedTexts) {
            if (TextUtils.isEmpty(recognizedText)) {
                continue;
            }
            serializedRecognizedText.append(recognizedText).append(" ");
        }
        return serializedRecognizedText.toString();
    }

    private static boolean isWavFile(String filePath) {
        return filePath.toLowerCase().endsWith(".wav");
    }
}
