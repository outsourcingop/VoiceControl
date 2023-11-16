package com.optoma.voicecontrol.model.azureopenai.chat;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ChatChoices {

   @SerializedName("message")
   @Expose
   public ChatMessage message;
}
