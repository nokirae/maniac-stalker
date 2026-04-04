package com.github.nokirae.maniacstalker;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.api.ClientModInitializer;

public class ManiacStalkerClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ChatReader.registerChatReader();
            TickBase.registerTickBasedReaders();
            Commands.registerCommands();
        });
    }
}