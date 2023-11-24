package com.optoma.voicecontrol.presenter;

import static com.optoma.voicecontrol.BuildConfig.SPEECH_SUBSCRPTION_KEY;
import static com.optoma.voicecontrol.util.FileUtil.createNewAudioFilePath;

import android.content.Context;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.optoma.voicecontrol.R;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

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
        void onTranscribed(String text, long timeStamp);

        void onAllPartsTranscribed(String transcribeResult);
    }
    private final String mSpeechRegion;
    private SpeechConfig mSpeechConfig;
    private AudioConfig mAudioInput;
    private SpeechRecognizer mSpeechRecognizer;

    private final CopyOnWriteArrayList<String> mCopyOnWriteTexts;

    public void startContinuousRecognitionAsync(List<String> absolutePathList, String currentLanguage) {
        Log.d(TAG, "startContinuousRecognitionAsync# currentLanguage=" + currentLanguage);

        if (!isWavFile(absolutePathList.get(0))) {
            String newFileAbsolutePath = createNewAudioFilePath(absolutePathList.get(0), 0);

            String command =
                    " -i " + absolutePathList.get(0) +
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
            startRecognize(currentLanguage, absolutePathList.get(0));
        }
    }

    private void startRecognize(String currentLanguage, String wavFileAbsPath) {
        mSpeechConfig = SpeechConfig.fromSubscription(SPEECH_SUBSCRPTION_KEY, mSpeechRegion);
        mSpeechConfig.setSpeechRecognitionLanguage(currentLanguage);
        mAudioInput = AudioConfig.fromWavFileInput(wavFileAbsPath);
        mSpeechRecognizer = new SpeechRecognizer(mSpeechConfig, mAudioInput);

        Future<SpeechRecognitionResult> task = mSpeechRecognizer.recognizeOnceAsync();

        mSpeechRecognizer.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            Log.d(TAG, "Intermediate result received: " + s);
            mBasicCallback.onLogReceived(s + " ");
        });

        mSpeechRecognizer.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
            String s = speechRecognitionResultEventArgs.getResult().getText();
            mCopyOnWriteTexts.add(s);
            mTranscribeCallback.onAllPartsTranscribed(s);
            Log.d(TAG, "Final result received: " + s);
            mBasicCallback.onLogReceived("Final result: " + s);
        });
    }

    private static boolean isWavFile(String filePath) {
        return filePath.toLowerCase().endsWith(".wav");
    }
}
