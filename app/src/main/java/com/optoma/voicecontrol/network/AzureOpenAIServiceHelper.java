package com.optoma.voicecontrol.network;

public class AzureOpenAIServiceHelper {
    public static final String BASE_URL =
            "https://iwcp-openai.openai.azure.com/openai/deployments/cc-chat-api/";

    private static volatile AzureOpenAIService instance;
    private static final Object lock = new Object();

    // Make the constructor private to prevent direct instantiation
    private AzureOpenAIServiceHelper() {
    }

    public static AzureOpenAIService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = NetworkServiceHelper.generateRetrofit(BASE_URL).create(AzureOpenAIService.class);
                }
            }
        }
        return instance;
    }
}
