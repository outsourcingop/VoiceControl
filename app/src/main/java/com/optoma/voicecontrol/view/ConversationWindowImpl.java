package com.optoma.voicecontrol.view;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;
import static android.content.Context.WINDOW_SERVICE;
import static com.optoma.voicecontrol.util.FileUtil.createScreenshotFile;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.optoma.voicecontrol.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ConversationWindowImpl implements ConversationWindow, View.OnLongClickListener {

    private final Context mContext;
    private final WindowManager mWm;
    private final WindowManager.LayoutParams mWindowLayoutParam;

    private boolean mWindowAdded;

    private OnWindowLongClickedListener mListener;
    private ViewGroup mRootView;
    private TextView mLogText;


    public ConversationWindowImpl(Context context) {
        mContext = context;
        mWm = (WindowManager) context.getSystemService(WINDOW_SERVICE);
        mWindowLayoutParam = createLayoutParams();

        setupUi();
    }

    private WindowManager.LayoutParams createLayoutParams() {
        // The screen height and width are calculated, cause
        // the height and width of the floating window is set depending on this
        DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        // WindowManager.LayoutParams takes a lot of parameters to set the
        // the parameters of the layout. One of them is Layout_type.
        int layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        // Now the Parameter of the floating-window layout is set.
        // 1) The Width of the window will be 55% of the phone width.
        // 2) The Height of the window will be 58% of the phone height.
        // 3) Layout_Type is already set.
        // 4) Next Parameter is Window_Flag. Here FLAG_NOT_FOCUSABLE is used. But
        // problem with this flag is key inputs can't be given to the EditText.
        // This problem is solved later.
        // 5) Next parameter is Layout_Format. System chooses a format that supports
        // translucency by PixelFormat.TRANSLUCENT
        WindowManager.LayoutParams windowLayoutParam = new WindowManager.LayoutParams(
                (int) (width * (0.95f)),
                (int) (height * (0.4f)),
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        // The Gravity of the Floating Window is set.
        // The Window will appear in the center of the screen
        windowLayoutParam.gravity = Gravity.BOTTOM;

        // X and Y value of the window is set
        windowLayoutParam.x = 0;
        windowLayoutParam.y = 0;

        return windowLayoutParam;
    }

    @Override
    public boolean addWindow() {
        if (mWindowAdded) {
            return false;
        }

        // The ViewGroup that inflates the floating_layout.xml is
        // added to the WindowManager with all the parameters
        mWm.addView(mRootView, mWindowLayoutParam);
        mWindowAdded = true;

        // Another feature of the floating window is, the window is movable.
        // The window can be moved at any position on the screen.
        mRootView.setOnTouchListener(new View.OnTouchListener() {
            final WindowManager.LayoutParams floatWindowLayoutUpdateParam = mWindowLayoutParam;
            double x;
            double y;
            double px;
            double py;

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                switch (event.getAction()) {
                    // When the window will be touched,
                    // the x and y position of that position
                    // will be retrieved
                    case MotionEvent.ACTION_DOWN:
                        x = floatWindowLayoutUpdateParam.x;
                        y = floatWindowLayoutUpdateParam.y;

                        // returns the original raw X
                        // coordinate of this event
                        px = event.getRawX();

                        // returns the original raw Y
                        // coordinate of this event
                        py = event.getRawY();
                        break;
                    // When the window will be dragged around,
                    // it will update the x, y of the Window Layout Parameter
                    case MotionEvent.ACTION_MOVE:
                        floatWindowLayoutUpdateParam.x = (int) ((x + event.getRawX()) - px);
                        floatWindowLayoutUpdateParam.y = -(int) ((y + event.getRawY()) - py);

                        // updated parameter is applied to the WindowManager
                        mWm.updateViewLayout(mRootView, floatWindowLayoutUpdateParam);
                        break;
                }
                return false;
            }
        });
        return true;
    }

    private void setupUi() {
        setupRootView();
        setupCloseButton();
        setupLogText();
    }

    private void setupRootView() {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                LAYOUT_INFLATER_SERVICE);
        mRootView = (ViewGroup) inflater.inflate(R.layout.conversation_window_layout, null);
        mRootView.setOnLongClickListener(this);
    }

    private void setupCloseButton() {
        mRootView.findViewById(R.id.closeButton).setOnClickListener(v -> removeWindow());
    }

    private void setupLogText() {
        mLogText = mRootView.findViewById(R.id.textLog);
        mLogText.setOnLongClickListener(this);
        if (false) {
            mLogText.setText(
                    "A\nB\nC\nD\nE\nF\nG\nH\nI\nJ\nK\nL\nM\nN\nO\nP\nQ\nR\nS\nT\nU\nV\nW\nX\nY\nZ");
        }
    }

    private File saveScreenshot() {
        // screenshot the root view as bitmap
        Bitmap screenshot = Bitmap.createBitmap(mLogText.getWidth(), mLogText.getHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(screenshot);
        mLogText.draw(canvas);

        Drawable logTextBackground = mLogText.getBackground();
        if (logTextBackground != null)
            logTextBackground.draw(canvas);
        else {
            canvas.drawColor(mContext.getColor(R.color.conversation_window_background));
        }
        mLogText.draw(canvas);

        // save it as file
        File screenshotFile = createScreenshotFile(mContext, System.currentTimeMillis());
        try (FileOutputStream out = new FileOutputStream(screenshotFile)) {
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return screenshotFile;
    }

    @Override
    public void removeWindow() {
        mWm.removeViewImmediate(mRootView);
        mWindowAdded = false;
    }

    @Override
    public boolean isWindowAdded() {
        return mWindowAdded;
    }

    @Override
    public void updateConversationOnWindow(String text) {
        String origText = mLogText.getText().toString();
        origText += "\n" + text;
        mLogText.setText(origText);
    }

    @Override
    public void setOnWindowLongClickedListener(OnWindowLongClickedListener listener) {
        mListener = listener;
    }

    @Override
    public boolean onLongClick(View v) {
        File screenshotFile = saveScreenshot();
        if (mListener != null) {
            mListener.onLongClicked(screenshotFile);
        }
        return true;
    }
}
