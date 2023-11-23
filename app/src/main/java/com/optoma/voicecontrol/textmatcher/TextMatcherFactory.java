package com.optoma.voicecontrol.textmatcher;

public class TextMatcherFactory {

    public static TextMatcher createTextMatcher(String language) {
        return new CosineSimilarityMatcher(language);
    }
}