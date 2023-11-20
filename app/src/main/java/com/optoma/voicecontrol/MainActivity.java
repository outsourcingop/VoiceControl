package com.optoma.voicecontrol;

import static com.optoma.voicecontrol.AiServiceProxy.KEY_AUDIO_FILE_PATH;
import static com.optoma.voicecontrol.AiServiceProxy.KEY_CALLBACK;
import static com.optoma.voicecontrol.AiServiceProxy.KEY_LANGUAGE;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_VC;
import static com.optoma.voicecontrol.util.DebugConfig.TAG_WITH_CLASS_NAME;
import static com.optoma.voicecontrol.util.FileUtil.createScreenshotFile;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.BinderThread;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.optoma.voicecontrol.state.ProcessState;
import com.optoma.voicecontrol.util.FileUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = TAG_WITH_CLASS_NAME ? "MainActivity" : TAG_VC;

    private static final int REQUEST_CODE_AUDIO_PICK = 1;

    private static final String ACTION_AI_SERVICE = "android.intent.action.AI_SERVICE";

    private final AiServiceProxy mAiServiceProxy = new AiServiceProxy();

    private boolean mAiServiceBound;

    private ProcessState mProcessState;

    private Spinner mLanguageSpinner;
    private Button mFileSelectorButton;
    private Button mRecordAudioButton;
    private Button mStopRecordingAudioButton;
    private Button mScreenshotButton;
    private ImageView mScreenshotImage;
    private TextView mLogText;
    private TextView mStatusText;

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

    private final ActivityResultLauncher<String> mRequestAudioPickPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.setType("audio/*");
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            startActivityForResult(intent, REQUEST_CODE_AUDIO_PICK);
                        } else {
                            String requestPermission;
                            if (Build.VERSION.SDK_INT >= 33) {
                                requestPermission = Manifest.permission.READ_MEDIA_AUDIO;
                            } else {
                                requestPermission = Manifest.permission.READ_EXTERNAL_STORAGE;
                            }
                            String logText = "Not allow the " + requestPermission + " permission";
                            updateLogText(logText);
                            Toast.makeText(MainActivity.this, logText, Toast.LENGTH_LONG).show();
                        }
                    });

    private final ActivityResultLauncher<String> mRequestAudioRecordPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            updateLogText("start audio recognition");
                            startAudioRecognition();
                        } else {
                            String logText = "Not allow the " +
                                    Manifest.permission.RECORD_AUDIO + " permission";
                            updateLogText(logText);
                            Toast.makeText(MainActivity.this, logText, Toast.LENGTH_LONG).show();
                        }
                    });

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        setupAudioFileSelection();
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
            //
            File screenshotFile = saveScreenshot(screenshot);
            sendWhiteBoardIntent(screenshotFile);
            return true;
        });
    }

    public static Bitmap takeScreenshot(Context context) {
        View rootView = ((ViewGroup) ((Activity) context).findViewById(
                android.R.id.content)).getChildAt(0);

        Bitmap screenshot = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(),
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(screenshot);
        rootView.draw(canvas);

        return screenshot;
    }

    private File saveScreenshot(Bitmap screenshot) {
        File screenshotFile = createScreenshotFile(this, System.currentTimeMillis());
        try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Save screenshot under the folder=" + screenshotFile.getPath());
        updateLogText("Save screenshot under the folder=" + screenshotFile.getPath());
        return screenshotFile;
    }

    private void sendWhiteBoardIntent(File file) {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(getString(R.string.intent_target_package_name),
                getString(R.string.intent_target_class_name)));
        intent.setDataAndType(Uri.fromFile(file), "image/*");
        Log.d(TAG, "Ready to start intent=" + intent);
        updateLogText("Ready to start intent=" + intent);
        // TODO
        // startActivity(intent);
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

    private void setupAudioFileSelection() {
        mFileSelectorButton = findViewById(R.id.buttonFromFile);
        mFileSelectorButton.setOnClickListener(view -> {
            if (Build.VERSION.SDK_INT >= 33) {
                mRequestAudioPickPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            } else {
                mRequestAudioPickPermissionLauncher.launch(
                        Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        });
    }

    private void setupSpeechRecognizer() {
        mRecordAudioButton = findViewById(R.id.record_audio_button);
        mRecordAudioButton.setOnClickListener(v -> mRequestAudioRecordPermissionLauncher.launch(
                Manifest.permission.RECORD_AUDIO));
        mStopRecordingAudioButton = findViewById(R.id.stop_recording_audio_button);
        mStopRecordingAudioButton.setOnClickListener(view -> {
            updateLogText("stop audio recognition");
            stopAudioRecognition();
        });
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
        mFileSelectorButton.setEnabled(interactWithUi);
        mScreenshotButton.setEnabled(interactWithUi);
        // Depends on the audio recognizing state
        if (state == ProcessState.START_AUDIO_RECOGNITION) {
            mRecordAudioButton.setEnabled(false);
            mStopRecordingAudioButton.setEnabled(true);
        } else if (state == ProcessState.STOP_AUDIO_RECOGNITION) {
            mRecordAudioButton.setEnabled(true);
            mStopRecordingAudioButton.setEnabled(false);
        } else {
            mRecordAudioButton.setEnabled(interactWithUi);
            mStopRecordingAudioButton.setEnabled(interactWithUi);
        }
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

    private void startAudioProcessing(String fileAbsolutePath) {
        Bundle bundle = new Bundle();
        bundle.putString(KEY_LANGUAGE, mLanguageSpinner.getSelectedItem().toString());
        bundle.putString(KEY_AUDIO_FILE_PATH, fileAbsolutePath);
        // Send the params to the service
        mAiServiceProxy.startAudioProcessing(bundle);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_AUDIO_PICK && resultCode == RESULT_OK) {
            if (data != null) {
                updateLogText("File picked, Uri = " + data.getData());
                Uri selectedAudioUri = data.getData();
                String fileAbsolutePath = FileUtil.getAbsolutePath(MainActivity.this,
                        selectedAudioUri);
                updateLogText("fileAbsolutePath = " + fileAbsolutePath);
                // start the audio file processing in the service
                startAudioProcessing(fileAbsolutePath);
            } else {
                Log.e(TAG, "onActivityResult: data is null!");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unbindAiService();
    }
}