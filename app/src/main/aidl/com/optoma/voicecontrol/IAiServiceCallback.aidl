// IAiServiceCallback.aidl
package com.optoma.voicecontrol;

// Declare any non-default types here with import statements

interface IAiServiceCallback {

    void onStateChanged(in String state) = 1;

    void onLogReceived(in String text) = 2;

    void onConversationReceived(in String text) = 3;

    void onSpeechRecognitionStoppingAutomatically() = 4;
}