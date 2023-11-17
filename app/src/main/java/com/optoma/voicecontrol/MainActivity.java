package com.optoma.voicecontrol;

import static com.optoma.voicecontrol.AiServiceProxy.KEY_CALLBACK;
import static com.optoma.voicecontrol.AiServiceProxy.KEY_LANGUAGE;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_MM;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_WITH_CLASS_NAME;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.BinderThread;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.optoma.voicecontrol.speechrecognizer.AudioTensorSource;
import com.optoma.voicecontrol.speechrecognizer.SpeechRecognizer;
import com.optoma.voicecontrol.state.ProcessState;
import com.optoma.voicecontrol.util.TextMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "MainActivity" : TAG_MM;

    private static final int RECORD_AUDIO_PERMISSION_REQUEST_CODE = 2;

    private static final String ACTION_AI_SERVICE = "android.intent.action.AI_SERVICE";

    private final AiServiceProxy mAiServiceProxy = new AiServiceProxy();

    private ExecutorService workerThreadExecutor = Executors.newSingleThreadExecutor();

    private AtomicBoolean stopRecordingFlag = new AtomicBoolean(false);

    private boolean mAiServiceBound;

    private ProcessState mProcessState;

    private Spinner mLanguageSpinner;
    private Button mRecordAudioButton;
    private Button mStopRecordingAudioButton;
    private Button mScreenshotButton;
    private ImageView mScreenshotImage;
    private TextView mLogText;
    private TextView mStatusText;

    private SpeechRecognizer speechRecognizer;

    private final ServiceConnection mAiServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected# name=" + name + ", binder=" + service);
            mAiServiceProxy.setProxy(IAiService.Stub.asInterface(service));
            mAiServiceBound = true;
            setupServiceCallback();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "onServiceDisconnected# name=" + name);
            mAiServiceProxy.setProxy(null);
            mAiServiceBound = false;
        }
    };

    private final IAiServiceCallback mAiServiceCallback = new IAiServiceCallback.Stub() {
        @Override
        @BinderThread
        public void onStateChanged(String state) {
            runOnUiThread(() -> {
                processStateChanged(ProcessState.valueOf(state));
                updateLogText("serviceState=" + mProcessState);
            });
        }

        @Override
        @BinderThread
        public void onLogReceived(String text) {
            runOnUiThread(() -> {
                Log.d(TAG, "onLogReceived# " + text);
                updateLogText(text);
            });
        }

        @Override
        @BinderThread
        public void onSummaryAndActionsReceived(String summary) {
            runOnUiThread(() -> {
                Log.d(TAG, "onSummaryAndActionsReceived#\n" + summary);
                updateLogText(summary);
            });
        }
    };

    private void setSuccessfulResult(final SpeechRecognizer.Result result) {
        runOnUiThread(() -> {
            mStatusText.setText(
                    "Successful speech recognition (" + result.getInferenceTimeInMs() + " ms)");
            if (result.getText().isEmpty()) {
                updateLogText("<No speech detected.>");
            } else {
                String recognitionResult = result.getText();
                updateLogText(recognitionResult);
                TextMatcher matcher = new TextMatcher();
                String matchResult = matcher.matchText(recognitionResult);
                updateLogText(matchResult);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private void initializeSpeechRecognizer() throws OrtException {
        InputStream inputStream = getResources().openRawResource(R.raw.whisper_cpu_int8_model);
        byte[] modelBytes;
        try {
            modelBytes = inputStream.readAllBytes();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        speechRecognizer = new SpeechRecognizer(modelBytes);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            initializeSpeechRecognizer();
        } catch (OrtException e) {
            throw new RuntimeException(e);
        }
        bindAiService();

        setContentView(R.layout.activity_main);
        setupContent();
        setupUi();
    }

    private void setupContent() {
    }

    private void setupUi() {
        setupLogText();
        setupLanguageSpinner();
        setupSpeechRecognizer();
        setupScreenshot();

        processStateChanged(ProcessState.IDLE);
    }

    private void setupScreenshot() {
        mScreenshotButton = findViewById(R.id.buttonScreenShot);
        mScreenshotImage = findViewById(R.id.imageScreenShot);

        mScreenshotButton.setOnLongClickListener(view -> {
            Bitmap screenshot = takeScreenshot(MainActivity.this);
            if (screenshot != null) {
                mScreenshotImage.setImageBitmap(screenshot);
            }
            return true;
        });
    }

    public static Bitmap takeScreenshot(Context context) {
        try {
            View rootView = ((ViewGroup) ((Activity) context).findViewById(
                    android.R.id.content)).getChildAt(0);

            Bitmap screenshot = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(),
                    Bitmap.Config.ARGB_8888);

            Canvas canvas = new Canvas(screenshot);
            rootView.draw(canvas);

            return screenshot;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void setupLogText() {
        mStatusText = findViewById(R.id.status_text);
        mLogText = findViewById(R.id.text_log);
        updateLogText("Log history:");
    }

    private void setupLanguageSpinner() {
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter<CharSequence> adapter =
                ArrayAdapter.createFromResource(this, R.array.languages,
                        android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears.
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner.
        mLanguageSpinner = findViewById(R.id.languageSpinner);
        mLanguageSpinner.setAdapter(adapter);
        mLanguageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                Log.d(TAG, "language=" + mLanguageSpinner.getSelectedItem());
                updateLogText("Choose language " + mLanguageSpinner.getSelectedItem());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Default language: "en-us"
        mLanguageSpinner.setSelection(adapter.getPosition("en-us"));
    }

    private void setupSpeechRecognizer() {
        mRecordAudioButton = findViewById(R.id.record_audio_button);
        mRecordAudioButton.setOnClickListener(view -> {
            if (!hasRecordAudioPermission()) {
                requestPermissions(
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        RECORD_AUDIO_PERMISSION_REQUEST_CODE
                );
                return;
            }

            // Disable audio buttons first.
            // The stop button will be enabled by the recording task.
            disableAudioButtons();

            workerThreadExecutor.submit(() -> {
                try {
                    stopRecordingFlag.set(false);
                    runOnUiThread(() -> mStopRecordingAudioButton.setEnabled(true));

                    OnnxTensor audioTensor = AudioTensorSource.Companion.fromRecording(
                            stopRecordingFlag);
                    SpeechRecognizer.Result result = speechRecognizer.run(audioTensor);
                    setSuccessfulResult(result);
                } catch (Exception e) {
//                    setError(e);
                } finally {
                    resetDefaultAudioButtonState();
                }
            });
        });

        mStopRecordingAudioButton = findViewById(R.id.stop_recording_audio_button);
        mStopRecordingAudioButton.setOnClickListener(view -> {
            // Disable audio buttons first.
            // The audio button state will be reset at the end of the record audio task.
            disableAudioButtons();

            stopRecordingFlag.set(true);
        });

        resetDefaultAudioButtonState();

    }

    private boolean hasRecordAudioPermission() {
        return ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST_CODE) {
            if (!hasRecordAudioPermission()) {
                Toast.makeText(
                        this,
                        "Permission to record audio was not granted.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }

    private void resetDefaultAudioButtonState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordAudioButton.setEnabled(true);
                mStopRecordingAudioButton.setEnabled(false);
            }
        });
    }

    private void disableAudioButtons() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mRecordAudioButton.setEnabled(false);
                mStopRecordingAudioButton.setEnabled(false);
            }
        });
    }

    private void startOrStopAudioRecording() {
        boolean isAudioRecognizing = mAiServiceProxy.isAudioRecognizing();
        if (isAudioRecognizing) {
            updateLogText("stop audio recognition");
            stopAudioRecognition();
//            mMicrophoneButton.setText("Start Audio Recognition by Microphone");
        } else {
            updateLogText("start audio recognition");
            startAudioRecognition();
//            mMicrophoneButton.setText("Stop Audio Recognition by Microphone");
        }
    }

    private void updateLogText(String text) {
        if (mLogText == null) {
            mLogText = findViewById(R.id.text_log);
        }
        final TextView v = mLogText;
        runOnUiThread(() -> {
            String origText = v.getText().toString();
            origText += "\n" + text;
            v.setText(origText);
        });
    }

    private void processStateChanged(ProcessState state) {
        Log.d(TAG, "processStateChanged# state from " + mProcessState + " to " + state);
        mProcessState = state;
        // Set all UIs enabled or disabled by the state.
        boolean interactWithUi = mProcessState.interactWithUi;
        mLanguageSpinner.setEnabled(interactWithUi);
//        mMicrophoneButton.setEnabled(interactWithUi);
    }

    private void bindAiService() {
        if (mAiServiceBound) {
            return;
        }
        Intent aiServiceIntent = new Intent(ACTION_AI_SERVICE);
        aiServiceIntent.setPackage(getPackageName());
        bindService(aiServiceIntent, mAiServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unbindAiService() {
        if (!mAiServiceBound) {
            return;
        }
        unbindService(mAiServiceConnection);
    }

    private void setupServiceCallback() {
        Bundle b = new Bundle();
        b.putBinder(KEY_CALLBACK, mAiServiceCallback.asBinder());
        // Prepare a callback for service that allows him to notify something back.
        mAiServiceProxy.initialize(b);
    }

    private void startAudioRecognition() {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_LANGUAGE, mLanguageSpinner.getSelectedItem().toString());
        // Send the params to the service
        mAiServiceProxy.startAudioRecognition(bundle);
    }

    private void stopAudioRecognition() {
        // Send the params to the service
        mAiServiceProxy.stopAudioRecognition(new Bundle());
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRecordingFlag.set(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindAiService();
        workerThreadExecutor.shutdown();
        speechRecognizer.close();
    }
}