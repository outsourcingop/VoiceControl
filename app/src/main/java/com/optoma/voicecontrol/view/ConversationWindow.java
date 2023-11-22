package com.optoma.voicecontrol.view;

import java.io.File;

public interface ConversationWindow {

    interface OnWindowLongClickedListener {
        void onLongClicked(File screenshotFile);
    }

    boolean addWindow();

    void removeWindow();

    boolean isWindowAdded();

    void updateConversationOnWindow(String text);

    void setOnWindowLongClickedListener(OnWindowLongClickedListener listener);
}
