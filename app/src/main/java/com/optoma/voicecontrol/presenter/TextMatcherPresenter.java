package com.optoma.voicecontrol.presenter;

import static com.optoma.voicecontrol.textmatcher.TextMatcherFactory.createTextMatcher;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

public class TextMatcherPresenter extends BasicPresenter {

    private final TextMatcherCallback mTextMatcherCallback;

    public interface TextMatcherCallback extends BasicCallback {
        void onTextMatched(String matchedText, String originalText);
    }

    public TextMatcherPresenter(Context context, TextMatcherCallback textMatcherCallback) {
        super(context, textMatcherCallback);
        TAG = TextMatcherPresenter.class.getSimpleName();
        mTextMatcherCallback = textMatcherCallback;
    }

    public void startTextMatching(String language, String recognizedText) {
        Log.d(TAG, "startTextMatching +++");
        String matchResult = createTextMatcher(language).matchText(recognizedText);
        if (TextUtils.isEmpty(matchResult)) {
            mBasicCallback.onLogReceived("NOT MATCH any actions.");
        } else {
            mBasicCallback.onLogReceived("MATCH the action=" + matchResult);
        }
        Log.d(TAG, "startTextMatching ---");
        mTextMatcherCallback.onTextMatched(matchResult, recognizedText);
    }
}
