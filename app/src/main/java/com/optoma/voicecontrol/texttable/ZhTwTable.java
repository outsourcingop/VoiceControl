package com.optoma.voicecontrol.texttable;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ZhTwTable extends TextTable {

    @Override
    public List<String> createInputSourceTable() {
        return Arrays.asList(
                "切換到HDMI 1",
                "切換到HDMI 2",
                "切換到HDMI 3",
                "切換到VGA",
                "切換到Type C",
                "切換到OPS",
                "切換到Android");
    }

    @Override
    protected List<String> createBrightnessTable() {
        return Arrays.asList(
                "亮一點",
                "暗一點",
                "亮度調到");
    }

    @Override
    protected List<String> createLowBlueLightTable() {
        return Arrays.asList(
                "低藍光調高",
                "低藍光調低",
                "低藍光調到",
                "低藍光關閉");
    }

    @Override
    protected List<String> createVolumeTable() {
        return Arrays.asList(
                "大聲一點",
                "小聲一點",
                "音量調到");
    }

    @Override
    protected List<String> createAudioMuteTable() {
        return Arrays.asList(
                "靜音",
                "音量恢復");
    }

    @Override
    protected List<String> createVideoMuteTable() {
        return Arrays.asList(
                "影像暫停",
                "恢復影像");
    }

    @Override
    protected List<String> createPowerTable() {
        return Arrays.asList(
                "關機",
                "開機");
    }

    @Override
    protected List<String> createRestartSystemTable() {
        return Collections.singletonList("重開機");
    }

    @Override
    protected List<String> createClipboardTable() {
        return Arrays.asList(
                "開啟白板",
                "關閉白板");
    }

    @Override
    protected List<String> createAirShareTable() {
        return Arrays.asList(
                "開啟Airshare",
                "關閉Airshare");
    }

    @Override
    protected List<String> createRecordingTable() {
        return Arrays.asList(
                "開始錄影",
                "結束錄影");
    }

    @Override
    protected List<String> createBrowserTable() {
        return Arrays.asList(
                "開啟瀏覽器",
                "關閉瀏覽器",
                "搜尋");
    }

    @Override
    protected List<String> createCalendarTable() {
        return Collections.singletonList("顯示行事曆");
    }

    @Override
    protected List<String> createBookingTable() {
        return Collections.singletonList("預訂時段");
    }

    @Override
    protected List<String> createSlidePageTable() {
        return Arrays.asList(
                "下一張",
                "上一張");
    }

    @NonNull
    @Override
    public String toString() {
        return "zh-tw";
    }
}
