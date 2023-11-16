package com.optoma.voicecontrol.state;

public enum ProcessState {
    IDLE(true),
    START_TRANSCRIBE(false),
    END_TRANSCRIBE(false),
    START_SUMMARY(false),
    END_SUMMARY(false),
    START_TEXT_SAVING(false),
    END_TEXT_SAVING(false);

    public final boolean interactWithUi;

    ProcessState(boolean interactWithUi) {
        this.interactWithUi = interactWithUi;
    }
}
