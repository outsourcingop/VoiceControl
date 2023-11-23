package com.optoma.voicecontrol.texttable;

import java.util.ArrayList;
import java.util.List;

public abstract class TextTable {

    public final List<String> mTextList;

    TextTable() {
        mTextList = new ArrayList<>();
        mTextList.addAll(createInputSourceTable());
        mTextList.addAll(createBrightnessTable());
        mTextList.addAll(createLowBlueLightTable());
        mTextList.addAll(createVolumeTable());
        mTextList.addAll(createAudioMuteTable());
        mTextList.addAll(createVideoMuteTable());
        mTextList.addAll(createPowerTable());
        mTextList.addAll(createRestartSystemTable());
        mTextList.addAll(createClipboardTable());
        mTextList.addAll(createAirShareTable());
        mTextList.addAll(createRecordingTable());
        mTextList.addAll(createBrowserTable());
        mTextList.addAll(createCalendarTable());
        mTextList.addAll(createBookingTable());
        mTextList.addAll(createSlidePageTable());
    }

    protected abstract List<String> createInputSourceTable();

    protected abstract List<String> createBrightnessTable();

    protected abstract List<String> createLowBlueLightTable();

    protected abstract List<String> createVolumeTable();

    protected abstract List<String> createAudioMuteTable();

    protected abstract List<String> createVideoMuteTable();

    protected abstract List<String> createPowerTable();

    protected abstract List<String> createRestartSystemTable();

    protected abstract List<String> createClipboardTable();

    protected abstract List<String> createAirShareTable();

    protected abstract List<String> createRecordingTable();

    protected abstract List<String> createBrowserTable();

    protected abstract List<String> createCalendarTable();

    protected abstract List<String> createBookingTable();

    protected abstract List<String> createSlidePageTable();

}
