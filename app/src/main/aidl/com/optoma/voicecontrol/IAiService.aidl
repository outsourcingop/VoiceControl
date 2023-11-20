// IAiService.aidl
package com.optoma.voicecontrol;

// Declare any non-default types here with import statements

interface IAiService {

    void initialize(in Bundle params) = 1;

    void startAudioProcessing(in Bundle params) = 2;

    // for the live audio processing
    void startAudioRecognition(in Bundle params) = 3;

    // for the live audio processing
    void stopAudioRecognition(in Bundle params) = 4;

    // for the live audio processing
    boolean isAudioRecognizing() = 5;
}