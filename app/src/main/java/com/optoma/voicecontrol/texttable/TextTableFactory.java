package com.optoma.voicecontrol.texttable;

import android.util.Log;

public class TextTableFactory {

    private static final String TAG = TextTableFactory.class.getSimpleName();

    public static TextTable createTextTable(String language) {
        TextTable textTable;
        switch (language) {
            case "en-us":
                textTable = new EnUsTable();
                break;
            case "zh-tw":
                textTable = new ZhTwTable();
                break;
            default:
                textTable = new DefaultTable();
                break;
        }
        Log.d(TAG, "createTextTable# table=" + textTable);
        return textTable;
    }
}
