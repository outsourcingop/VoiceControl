package com.optoma.voicecontrol.presenter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.optoma.voicecontrol.LogTextCallback;
import com.optoma.voicecontrol.util.TextMatcher;

import java.util.ArrayList;

public class TextMatcherPresenter extends BasicPresenter {

    private final TextMatcherCallback mTextMatcherCallback;

    public interface TextMatcherCallback extends ErrorCallback {
        void onTextMatched(String matchedText, ArrayList<String> texts);
    }

    public TextMatcherPresenter(Context context, LogTextCallback callback,
            TextMatcherCallback textMatcherCallback) {
        super(context, callback, textMatcherCallback);
        TAG = TextMatcherPresenter.class.getSimpleName();
        mTextMatcherCallback = textMatcherCallback;
    }

    public void startTextMatching(String language, ArrayList<String> texts) {
        Log.d(TAG, "startTextMatching +++");
        TextMatcher matcher = new TextMatcher();
        String matchResult = texts.size() == 1 ? matcher.matchText(language, texts.get(0)) : "";
        if (TextUtils.isEmpty(matchResult)) {
            mLogTextCallback.onLogReceived("NOT MATCH any actions.");
            mLogTextCallback.onLogReceived("Should go to the next presenter");
        } else {
            mLogTextCallback.onLogReceived("MATCH the action=" + matchResult);
        }
        Log.d(TAG, "startTextMatching ---");
        mTextMatcherCallback.onTextMatched(matchResult, texts);
    }
}
