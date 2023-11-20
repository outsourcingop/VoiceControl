package com.optoma.voicecontrol.presenter;

import android.content.Context;
import android.util.Log;

import com.optoma.voicecontrol.BuildConfig;
import com.optoma.voicecontrol.LogTextCallback;
import com.optoma.voicecontrol.R;
import com.optoma.voicecontrol.model.azureopenai.chat.ChatMessage;
import com.optoma.voicecontrol.model.azureopenai.chat.ChatRequest;
import com.optoma.voicecontrol.model.azureopenai.chat.ChatResponse;
import com.optoma.voicecontrol.network.AzureOpenAIServiceHelper;
import com.optoma.voicecontrol.network.NetworkServiceHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class ChatPresenter extends BasicPresenter {

    private final ChatPresenter.ChatCallback mChatCallback;
    public interface ChatCallback extends ErrorCallback {
        void onChatResponse(String response);
    }
    public ChatPresenter(Context context, LogTextCallback callback, ChatCallback chatCallback) {
        super(context, callback, chatCallback);

        TAG = ChatPresenter.class.getSimpleName();
        mChatCallback = chatCallback;
    }

    public void getChatResponse(String language, String conversation) {
        Log.d(TAG, "sendMessageToAzureOpenAI# " + "\n" + conversation);
        mLogTextCallback.onLogReceived(
                "sendMessageToAzureOpenAI -- Start");

        final int maxTokens = mContext.getResources().getInteger(R.integer.max_summary_tokens);

        List<ChatMessage> messages = new ArrayList<>();
        if (language.equalsIgnoreCase("en-us")) {
            messages.add(new ChatMessage("system",
                    "You are an AI assistant MUST USE english to reply."));
        } else {
            messages.add(new ChatMessage("system",
                    "您是一個AI助手，必須使用 '''繁體中文''' 回覆。" ));
        }
        messages.add(new ChatMessage("user", conversation));

        mCompositeDisposable.add(
                AzureOpenAIServiceHelper.getInstance()
                        .chatAzureOpenAI(BuildConfig.AZURE_OPEN_AI_API_KEY, "application/json",
                                new ChatRequest(messages))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .timeout(2, TimeUnit.MINUTES)
                        .subscribe(response -> {
                            mLogTextCallback.onLogReceived(
                                    "sendMessageToAzureOpenAI -- End");
                            Log.d(TAG, "onResponse: " + response.code());
                            if (response.isSuccessful()) {
                                ChatResponse result = response.body();
                                String chatResponse = result.choices.get(0).message.content;
                                Log.d(TAG, "onResponse: " + chatResponse);
                                mLogTextCallback.onLogReceived("" +
                                        "===== \n AI:\n" + chatResponse + "\n=====");
                                mChatCallback.onChatResponse(chatResponse);
                            } else {
                                ResponseBody errorBody = response.errorBody();
                                if (errorBody != null) {
                                    String errorLog = "errorMessage: " +
                                            NetworkServiceHelper.generateErrorToastContent(errorBody);
                                    performError(errorLog);
                                }
                            }
                        }, throwable -> {
                            String errorLog = "errorMessage: " +
                                    "fail to getOpenAICompletion. " + throwable.getMessage();
                            performError(errorLog);
                        })
        );
    }
}
