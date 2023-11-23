package com.optoma.voicecontrol.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextMatcher {
    public static final double THRESHOLD = 0.5;
    private List<String> chineseTextTable;
    private List<String> englishTextTable;

    public TextMatcher() {
        chineseTextTable = new ArrayList<>();

        //  Input Source change
        chineseTextTable.add("切換到HDMI 1");
        chineseTextTable.add("切換到HDMI 2");
        chineseTextTable.add("切換到HDMI 3");
        chineseTextTable.add("切換到VGA");
        chineseTextTable.add("切換到Type C");
        chineseTextTable.add("切換到OPS");
        chineseTextTable.add("切換到Android");
        //  Brightness change
        chineseTextTable.add("亮一點");
        chineseTextTable.add("暗一點");
        chineseTextTable.add("亮度調到");
        //  Low Blue Light change
        chineseTextTable.add("低藍光調高");
        chineseTextTable.add("低藍光調低");
        chineseTextTable.add("低藍光調到");
        chineseTextTable.add("低藍光關閉");
        //  Volume change
        chineseTextTable.add("大聲一點");
        chineseTextTable.add("小聲一點");
        chineseTextTable.add("音量調到");
        //  Audio mute on/off
        chineseTextTable.add("靜音");
        chineseTextTable.add("音量恢復");
        //  Video mute on/off
        chineseTextTable.add("影像暫停");
        chineseTextTable.add("恢復影像");
        //  Power off
        chineseTextTable.add("關機");
        //  Power on
        chineseTextTable.add("開機");
        //  Restart
        chineseTextTable.add("重開機");
        //  CB on/off
        chineseTextTable.add("開啟白板");
        chineseTextTable.add("關閉白板");
        //  Airshareon/off
        chineseTextTable.add("開啟Airshare");
        chineseTextTable.add("關閉Airshare");
        //  Start/Stop recording
        chineseTextTable.add("開始錄影");
        chineseTextTable.add("結束錄影");
        //  Browser on/off/search
        chineseTextTable.add("開啟瀏覽器");
        chineseTextTable.add("關閉瀏覽器");
        chineseTextTable.add("搜尋");
        //  Show meeting room calendar
        chineseTextTable.add("顯示行事曆");
        //  Book meeting room
        chineseTextTable.add("預訂時段");
        //  Slide page down/up
        chineseTextTable.add("下一張");
        chineseTextTable.add("上一張");

        englishTextTable = new ArrayList<>();
        // Input Source change
        englishTextTable.add("Switch to HDMI 1");
        englishTextTable.add("Switch to HDMI 2");
        englishTextTable.add("Switch to HDMI 3");
        englishTextTable.add("Switch to VGA");
        englishTextTable.add("Switch to Type C");
        englishTextTable.add("Switch to OPS");
        englishTextTable.add("Switch to Android");
        // Brightness change
        englishTextTable.add("Brightness up");
        englishTextTable.add("Brightness down");
        englishTextTable.add("Brightness set to");
        // Low Blue Light change
        englishTextTable.add("Low Blue Light up");
        englishTextTable.add("Low Blue Light down");
        englishTextTable.add("Low Blue Light set to");
        englishTextTable.add("Low Blue Light off");
        // Volume change
        englishTextTable.add("Volume up");
        englishTextTable.add("Volume down");
        englishTextTable.add("Volume set to");
        // Audio mute on/off
        englishTextTable.add("Audio mute");
        englishTextTable.add("Audio unmute");
        // Video mute on/off
        englishTextTable.add("Video mute");
        englishTextTable.add("Video unmute");
        // Power off
        englishTextTable.add("Power off");
        // Power on
        englishTextTable.add("Power on");
        // Restart
        englishTextTable.add("Restart");
        // CB on/off
        englishTextTable.add("Launch CB");
        englishTextTable.add("Close CB");
        // Airshare on/off
        englishTextTable.add("Launch Airshare");
        englishTextTable.add("Close Airshare");
        // Start/Stop recording
        englishTextTable.add("Start recording");
        englishTextTable.add("Stop recording");
        // Browser on/off/search
        englishTextTable.add("Launch Browser");
        englishTextTable.add("Close Browser");
        englishTextTable.add("Search");
        // Show meeting room calendar
        englishTextTable.add("Show calendar");
        // Book meeting room
        englishTextTable.add("Book Period");
        // Slide page down/up
        englishTextTable.add("Page down");
        englishTextTable.add("Page up");
    }

    public String matchText(String language, String inputText) {
        inputText = inputText.toLowerCase().replaceAll("\\s+", "");

        if (language.equalsIgnoreCase("en-us")) {
            for (String text : englishTextTable) {
                String normalizedText = text.toLowerCase().replaceAll("\\s+", "");
                if (inputText.equals(normalizedText) || inputText.contains(normalizedText)) {
                    return text;
                }
                double similarity = cosineSimilarity(inputText, normalizedText);
                if (similarity > THRESHOLD) {
                    return text;
                }
            }
        } else {
            for (String text : chineseTextTable) {
                String normalizedText = text.toLowerCase().replaceAll("\\s+", "");
                if (inputText.equals(normalizedText) || inputText.contains(normalizedText)) {
                    return text;
                }
                double similarity = cosineSimilarity(inputText, normalizedText);
                if (similarity > THRESHOLD) {
                    return text;
                }
            }
        }

        return "";
    }

    public static double cosineSimilarity(String str1, String str2) {
        Map<String, Integer> vector1 = getTermFrequencyVector(str1);
        Map<String, Integer> vector2 = getTermFrequencyVector(str2);

        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;

        for (String term : vector1.keySet()) {
            if (vector2.containsKey(term)) {
                dotProduct += vector1.get(term) * vector2.get(term);
            }
            magnitude1 += Math.pow(vector1.get(term), 2);
        }

        for (String term : vector2.keySet()) {
            magnitude2 += Math.pow(vector2.get(term), 2);
        }

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0; // To handle division by zero
        }

        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }

    private static Map<String, Integer> getTermFrequencyVector(String text) {
        Map<String, Integer> vector = new HashMap<>();
        String[] terms = text.split("\\s+");
        for (String term : terms) {
            vector.put(term, vector.getOrDefault(term, 0) + 1);
        }
        return vector;
    }

}
