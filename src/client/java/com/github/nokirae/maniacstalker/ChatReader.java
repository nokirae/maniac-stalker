package com.github.nokirae.maniacstalker;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;

public class ChatReader {
    public static void registerChatReader() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String chatMessage = message.getString();
            Variables.lastMessages.put(chatMessage, Variables.messagesTimeOut);
        });
    }
}
