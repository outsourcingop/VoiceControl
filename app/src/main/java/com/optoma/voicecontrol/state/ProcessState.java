package com.optoma.voicecontrol.state;

public enum ProcessState {
    IDLE(true),
    START_TRANSCRIBE(false),
    END_TRANSCRIBE(false),
    START_SUMMARY(false),
    END_SUMMARY(false),
    START_AUDIO_RECOGNITION(false),
    STOP_AUDIO_RECOGNITION(false),
    START_TEXT_MATCHING(false),
    END_TEXT_MATCHING(false);

    public final boolean interactWithUi;

    ProcessState(boolean interactWithUi) {
        this.interactWithUi = interactWithUi;
    }
}
