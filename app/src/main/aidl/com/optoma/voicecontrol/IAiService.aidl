// IAiService.aidl
package com.optoma.voicecontrol;

// Declare any non-default types here with import statements

interface IAiService {

    void initialize(in Bundle params) = 1;

    void startAudioProcessing(in Bundle params) = 2;

    void startTextProcessing(in Bundle params) = 3;
}