package com.optoma.voicecontrol.view;

import android.content.Context;

public class ConversationWindowFactory {

    public static ConversationWindow createConversationWindow(Context context) {
        return new ConversationWindowImpl(context);
    }
}
