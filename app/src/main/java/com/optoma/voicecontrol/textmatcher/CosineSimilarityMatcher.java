package com.optoma.voicecontrol.textmatcher;

import static com.optoma.voicecontrol.texttable.TextTableFactory.createTextTable;

import com.optoma.voicecontrol.texttable.TextTable;

import java.util.HashMap;
import java.util.Map;

public class CosineSimilarityMatcher implements TextMatcher {

    private static final double THRESHOLD = 0.5;

    private final TextTable mTable;


    CosineSimilarityMatcher(String language) {
        mTable = createTextTable(language);
    }

    @Override
    public String matchText(String input) {
        input = input.toLowerCase().replaceAll("\\s+", "");
        for (String text : mTable.mTextList) {
            String normalizedText = text.toLowerCase().replaceAll("\\s+", "");
            if (input.equals(normalizedText) || input.contains(normalizedText)) {
                return text;
            }
            double similarity = cosineSimilarity(input, normalizedText);
            if (similarity > THRESHOLD) {
                return text;
            }
        }
        return null;
    }

    private double cosineSimilarity(String str1, String str2) {
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

    private Map<String, Integer> getTermFrequencyVector(String text) {
        Map<String, Integer> vector = new HashMap<>();
        String[] terms = text.split("\\s+");
        for (String term : terms) {
            vector.put(term, vector.getOrDefault(term, 0) + 1);
        }
        return vector;
    }
}
