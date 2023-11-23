package com.optoma.voicecontrol.texttable;

import androidx.annotation.NonNull;

import com.optoma.voicecontrol.texttable.TextTable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EnUsTable extends TextTable {

    @Override
    public List<String> createInputSourceTable() {
        return Arrays.asList(
                "Switch to HDMI 1",
                "Switch to HDMI 2",
                "Switch to HDMI 3",
                "Switch to VGA",
                "Switch to Type C",
                "Switch to OPS",
                "Switch to Android");
    }

    @Override
    protected List<String> createBrightnessTable() {
        return Arrays.asList(
                "Brightness up",
                "Brightness down",
                "Brightness set to");
    }

    @Override
    protected List<String> createLowBlueLightTable() {
        return Arrays.asList(
                "Low Blue Light up",
                "Low Blue Light down",
                "Low Blue Light set to",
                "Low Blue Light off");
    }

    @Override
    protected List<String> createVolumeTable() {
        return Arrays.asList(
                "Volume up",
                "Volume down",
                "Volume set to");
    }

    @Override
    protected List<String> createAudioMuteTable() {
        return Arrays.asList(
                "Audio mute",
                "Audio unmute");
    }

    @Override
    protected List<String> createVideoMuteTable() {
        return Arrays.asList(
                "Video mute",
                "Video unmute");
    }

    @Override
    protected List<String> createPowerTable() {
        return Arrays.asList(
                "Power off",
                "Power on");
    }

    @Override
    protected List<String> createRestartSystemTable() {
        return Collections.singletonList("Restart");
    }

    @Override
    protected List<String> createClipboardTable() {
        return Arrays.asList(
                "Launch CB",
                "Close CB");
    }

    @Override
    protected List<String> createAirShareTable() {
        return Arrays.asList(
                "Launch Airshare",
                "Close Airshare");
    }

    @Override
    protected List<String> createRecordingTable() {
        return Arrays.asList(
                "Start recording",
                "Stop recording");
    }

    @Override
    protected List<String> createBrowserTable() {
        return Arrays.asList(
                "Launch Browser",
                "Close Browser",
                "Search");
    }

    @Override
    protected List<String> createCalendarTable() {
        return Collections.singletonList("Show calendar");
    }

    @Override
    protected List<String> createBookingTable() {
        return Collections.singletonList("Book Period");
    }

    @Override
    protected List<String> createSlidePageTable() {
        return Arrays.asList(
                "Page down",
                "Page up");
    }

    @NonNull
    @Override
    public String toString() {
        return "en-us";
    }
}
