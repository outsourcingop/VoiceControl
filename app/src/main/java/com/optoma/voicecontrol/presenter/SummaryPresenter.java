package com.optoma.voicecontrol.presenter;

import static com.optoma.voicecontrol.util.FileUtil.createMeetingActionsFile;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.ResponseBody;

public class SummaryPresenter extends BasicPresenter {

    private final SummaryCallback mSummaryCallback;

    private final Map<Integer, String> mPartNumberToSummary = new ConcurrentHashMap<>();

    public interface SummaryCallback extends ErrorCallback {
        void onSummarized();
    }

    public SummaryPresenter(Context context, LogTextCallback callback,
            SummaryCallback summaryCallback) {
        super(context, callback, summaryCallback);
        TAG = SummaryPresenter.class.getSimpleName();
        mSummaryCallback = summaryCallback;
    }

    public void processMultipleConversations(String currentLanguage,
            Map<Integer, String> partNumberToConversations, long timeStamp) {
        for (int i = 0; i < partNumberToConversations.size(); i++) {
            getSummary(currentLanguage, i, partNumberToConversations.get(i), timeStamp,
                    partNumberToConversations.size());
        }
    }

    public void getSummary(String language, int partNumber, String conversation, long timeInMillis,
            int totalPartNumber) {
        Log.d(TAG, "sendMessageToAzureOpenAI# filePartNumber=" + partNumber + "\n" +
                conversation + ", " + "\ntimestamp" + ": " + timeInMillis);
        mLogTextCallback.onLogReceived(
                "sendMessageToAzureOpenAI(filePartNumber=" + partNumber + ") -- Start");

        final int maxTokens = mContext.getResources().getInteger(R.integer.max_summary_tokens);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
                "You are an AI assistant that helps people generate a summary in /// /// and list all action items discussed." +
                        "All in " + maxTokens + " tokens. You MUST Follow the format in {{{ }}} and MUST USE " + language + " LANGUAGE." +
                        "{{{\nSummary:\n(Summary) \n\nAction items:\n- Describe any additional action items here.}}}"));
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
                                    "sendMessageToAzureOpenAI(filePartNumber=" + partNumber + ") -- End");
                            Log.d(TAG, "onResponse: " + response.code());
                            if (response.isSuccessful()) {
                                ChatResponse result = response.body();
                                String meetingSummary = result.choices.get(0).message.content;
                                Log.d(TAG, "onResponse: " + meetingSummary);
                                mPartNumberToSummary.put(partNumber, meetingSummary);
                                if (mPartNumberToSummary.size() == totalPartNumber) {
                                    storeMeetingActionsToFile(timeInMillis);
                                }
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

    private void storeMeetingActionsToFile(long timestamp) {
        Log.d(TAG, "start to store meeting actions to database");
        mLogTextCallback.onLogReceived("storeMeetingActionsToDatabase");

        File outputFile = createMeetingActionsFile(mContext, timestamp);

        mCompositeDisposable.add(
                Completable.fromAction(() -> {
                            StringBuilder summary = new StringBuilder();
                            List<String> summaryList = new ArrayList<>();
                            List<String> actionItemsList = new ArrayList<>();

                            parseSummaryAndActionItems(summaryList, actionItemsList);

                            try {
                                FileOutputStream outputStream = new FileOutputStream(outputFile);
                                outputStream.write("Summary:\n".getBytes());
                                summary.append("Summary:\n");
                                for (int i = 0; i < summaryList.size(); i++) {
                                    outputStream.write(summaryList.get(i).getBytes());
                                    outputStream.write("\n".getBytes());
                                    summary.append(summaryList.get(i)).append("\n");
                                }
                                outputStream.write("\nAction items:\n".getBytes());
                                summary.append("\nAction items:\n");
                                for (int i = 0; i < actionItemsList.size(); i++) {
                                    outputStream.write(actionItemsList.get(i).getBytes());
                                    outputStream.write("\n".getBytes());
                                    summary.append(actionItemsList.get(i)).append("\n");
                                }

                                outputStream.close();
                                mLogTextCallback.onLogReceived(summary.toString());

                            } catch (IOException e) {
                                String errorLog = "errorMessage: IOException";
                                performError(errorLog);
                            }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(() -> {
                            String saveFileLog = "***** Saved meeting actions to " +
                                    outputFile.getPath() + " *****\n";
                            Log.d(TAG, saveFileLog);
                            mLogTextCallback.onLogReceived(saveFileLog);
                            mSummaryCallback.onSummarized();
                        })
        );
    }

    private void parseSummaryAndActionItems(List<String> summaryList,
            List<String> actionItemsList) {
        for (int i = 0; i < mPartNumberToSummary.size(); i++) {
            String input = mPartNumberToSummary.get(i);
            if (input != null) {
                String[] parts = input.split("Action items:");
                if (parts.length == 2) {
                    String summary = parts[0].trim().replace("Summary:", "").trim();
                    String actionItems = parts[1].trim();
                    summaryList.add(summary);
                    actionItemsList.add(actionItems);
                }
            }
        }
    }
}
