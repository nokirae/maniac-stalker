package com.github.nokirae.maniacstulker;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class ManiacStalkerClient implements ClientModInitializer {
        public boolean Debug = false;
    	public static final String MOD_ID = "maniac-stalker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
        private Map<UUID, String> lastPlayerMap = new HashMap<>();
        private Map<String, String> lastGameModeMap = new HashMap<>();
        private Map<String, Integer> lastMessages = new HashMap<>();
        public int LastNearPlayers = 1;
        private int messagesTimeOut = 5; // seconds
        private boolean moduleJoinLeave = true;
        private boolean moduleGameMode = true;
        private boolean moduleNear = true;
        
	@Override
	public void onInitializeClient() {
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (Debug) {
                LOGGER.info("Connected!");
            }
            
            registerChatReader();
            registerTickBasedReaders();
            registerPingCommand();
        });
    }
    
    private void registerChatReader() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String chatMessage = message.getString();
            if (Debug){
                LOGGER.info("ChatReader | " + chatMessage);
            }
            lastMessages.put(chatMessage, messagesTimeOut);
        });
    }
    
    
    private void registerPingCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ms")
                .then(literal("ping")
                    .executes(context -> {
                        final MinecraftClient client = context.getSource().getClient();
                        if (client.player == null || client.getNetworkHandler() == null) {
                            context.getSource().sendError(Text.literal("Not connected..."));
                            return 0;
                        }

                        Collection<PlayerListEntry> players = client.getNetworkHandler().getPlayerList();
                        if (players.isEmpty()) {
                            context.getSource().sendFeedback(Text.literal("No players.").formatted(Formatting.YELLOW));
                            return 1;
                        }
                        
                        List<PlayerListEntry> sortedPlayers = new ArrayList<>(players);
                        sortedPlayers.sort(Comparator.comparingInt(PlayerListEntry::getLatency));

                        MutableText fullMessage = Text.literal("").formatted(Formatting.GOLD);
                        for (PlayerListEntry playerEntry : sortedPlayers) {
                            String name = playerEntry.getProfile().getName();
                            int latency = playerEntry.getLatency();
                            fullMessage.append(Text.literal("⏺ ").formatted(Formatting.AQUA));
                            fullMessage.append(Text.literal(name).formatted(Formatting.GOLD));
                            fullMessage.append(Text.literal(" - ").formatted(Formatting.GRAY));
                            fullMessage.append(formatPing(latency));
                            fullMessage.append(Text.literal("\n").formatted(Formatting.GRAY));
                        }
                        
                        context.getSource().sendFeedback(fullMessage);
                        return 1;
                    })
                    .then(argument("player", StringArgumentType.greedyString())
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            final MinecraftClient client = context.getSource().getClient();

                            if (client.player == null || client.getNetworkHandler() == null) {
                                context.getSource().sendError(Text.literal("Not connected..."));
                                return 0;
                            }

                            PlayerListEntry targetPlayer = client.getNetworkHandler().getPlayerList().stream()
                                    .filter(p -> p.getProfile().getName().equalsIgnoreCase(playerName))
                                    .findFirst()
                                    .orElse(null);

                            if (targetPlayer != null) {
                                MutableText message = Text.literal("☑ ").formatted(Formatting.AQUA)
                                        .append(Text.literal(playerName).formatted(Formatting.GOLD))
                                        .append(Text.literal(" - ").formatted(Formatting.GRAY))
                                        .append(formatPing(targetPlayer.getLatency()));
                                context.getSource().sendFeedback(message);
                            } else {
                                context.getSource().sendError(Text.literal("☒ " + playerName));
                            }
                            return 1;
                        }))
                )
                .then(literal("check").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Not connected..."));
                        return 0;
                    }

                    int tabCount = client.getNetworkHandler().getPlayerList().size();
                    int worldCount = client.world.getPlayers().size();

                    MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA);

                    message.append(Text.literal("TAB: ").formatted(Formatting.GRAY))
                           .append(Text.literal(String.valueOf(tabCount)).formatted(Formatting.YELLOW));

                    message.append(Text.literal(" World: ").formatted(Formatting.GRAY))
                           .append(Text.literal(String.valueOf(worldCount)).formatted(Formatting.YELLOW));

                    if (tabCount == worldCount) {
                        message.append(Text.literal(" ☑").formatted(Formatting.GREEN));
                    } else {
                        message.append(Text.literal(" ☒").formatted(Formatting.RED));
                    }

                    context.getSource().sendFeedback(message);
                    return 1;
                }))
                .then(literal("near").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Not connected..."));
                        return 0;
                    }

                    List<AbstractClientPlayerEntity> Near = client.world.getPlayers();
                    
                    for (AbstractClientPlayerEntity ACPE : Near) {
                        MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA)
                                .append(Text.literal(ACPE.getGameProfile().getName()).formatted(Formatting.GREEN));
                        client.player.sendMessage(message, false);
                    }

                    return 1;
                }))
                .then(literal("list").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Not connected..."));
                        return 0;
                    }

                    Collection<PlayerListEntry> Players = client.getNetworkHandler().getPlayerList();
                    
                    for (PlayerListEntry PLE : Players) {
                        MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA);
                        message.append(Text.literal(PLE.getProfile().getName()).formatted(Formatting.GREEN));
                        message.append(Text.literal(" | "+ PLE.getGameMode().asString() + " | " + PLE.getLatency() + " ⏳").formatted(Formatting.GRAY));
                        message.append(Text.literal("\n" + PLE.getProfile().getId().toString() + "\n").formatted(Formatting.GRAY));
                        client.player.sendMessage(message, false);
                    }

                    return 1;
                }))
                .then(literal("toggle").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Not connected..."));
                        return 0;
                    }

                    moduleJoinLeave = !moduleJoinLeave;
                    moduleGameMode = !moduleGameMode;
                    moduleNear = !moduleNear;
                    
                    MutableText message = Text.literal("⏺").formatted(Formatting.AQUA);
                    
                    if (moduleJoinLeave) {
                        message.append(Text.literal(" JoinLeave").formatted(Formatting.GREEN));
                    }
                    else {
                        message.append(Text.literal(" JoinLeave").formatted(Formatting.RED));
                    }
                    
                    if (moduleGameMode) {
                        message.append(Text.literal(" GameMode").formatted(Formatting.GREEN));
                    }
                    else {
                        message.append(Text.literal(" GameMode").formatted(Formatting.RED));
                    }
                    
                    if (moduleNear) {
                        message.append(Text.literal(" Near").formatted(Formatting.GREEN));
                    }
                    else {
                        message.append(Text.literal(" Near").formatted(Formatting.RED));
                    }
                    
                    client.player.sendMessage(message, false);
                    
                    return 1;
                })
                    .then(argument("module", StringArgumentType.greedyString())
                        .executes(context -> {
                            String moduleName = StringArgumentType.getString(context, "module");
                            final MinecraftClient client = context.getSource().getClient();

                            if (client.player == null || client.getNetworkHandler() == null) {
                                context.getSource().sendError(Text.literal("Not connected..."));
                                return 0;
                            }
                            
                            MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA);
                            
                            switch (moduleName.toLowerCase()) {
                                case "joinleave":
                                    moduleJoinLeave = !moduleJoinLeave;
                                    if (moduleJoinLeave) {
                                        message.append(Text.literal("JoinLeave").formatted(Formatting.GREEN));
                                    }
                                    else {
                                        message.append(Text.literal("JoinLeave").formatted(Formatting.RED));
                                    }
                                    break;
                                case "gamemode":
                                    moduleGameMode = !moduleGameMode;
                                    if (moduleGameMode) {
                                        message.append(Text.literal("GameMode").formatted(Formatting.GREEN));
                                    }
                                    else {
                                        message.append(Text.literal("GameMode").formatted(Formatting.RED));
                                    }
                                    break;
                                case "near":
                                    moduleNear = !moduleNear;
                                    if (moduleNear) {
                                        message.append(Text.literal("Near").formatted(Formatting.GREEN));
                                    }
                                    else {
                                        message.append(Text.literal("Near").formatted(Formatting.RED));
                                    }
                                    break;
                                default:
                                    message.append(Text.literal("JoinLeave | GameMode | Near").formatted(Formatting.YELLOW));
                                    
                            }
                            
                            client.player.sendMessage(message, false);
                            
                            return 1;
                        })
                    )
                )
                .then(literal("uwu").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Not connected..."));
                        return 0;
                    }

                    Collection<PlayerListEntry> Players = client.getNetworkHandler().getPlayerList();
                    
                    for (PlayerListEntry PLE : Players) {
                        MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA);
                        message.append(Text.literal(PLE.getProfile().getName()).formatted(Formatting.GREEN));
                        message.append(Text.literal(" - ").formatted(Formatting.GRAY));
                        Text displayNameComponent = PLE.getDisplayName();
    
                        String fullRowText;

                        if (displayNameComponent != null) {
                            fullRowText = displayNameComponent.getString();
                        } else {
                            fullRowText = PLE.getProfile().getName();
                        }
                        
                        if (fullRowText.contains("♂")) {
                            if (PLE.getSkinTextures().model().getName().contains("slim")) {
                                message.append(Text.literal("Femboy Material").formatted(Formatting.GOLD));
                            }

                            else {
                                message.append(Text.literal("Boy").formatted(Formatting.AQUA));
                            }
                        }
                        
                        else if (fullRowText.contains("♀")) {
                            if (PLE.getSkinTextures().model().getName().contains("slim")) {
                                message.append(Text.literal("Girl").formatted(Formatting.LIGHT_PURPLE));
                            }

                            else {
                                message.append(Text.literal("Tomboy Material").formatted(Formatting.GOLD));
                            }
                        }
                        
                        else if (fullRowText.contains("⚥")) {
                            if (PLE.getSkinTextures().model().getName().contains("slim")) {
                                message.append(Text.literal("Femboy").formatted(Formatting.LIGHT_PURPLE));
                            }

                            else {
                                message.append(Text.literal("Tomboy").formatted(Formatting.AQUA));
                            }
                        }
                        
                        else {
                            if (PLE.getSkinTextures().model().getName().contains("slim")) {
                                message.append(Text.literal("Girl (Femboy?)").formatted(Formatting.LIGHT_PURPLE));
                            }

                            else {
                                message.append(Text.literal("Boy (Tomboy?)").formatted(Formatting.AQUA));
                            }
                        }
                        
                        client.player.sendMessage(message, false);
                    }

                    return 1;
                }))
            );
        });
    }
    
    private Text formatPing(int latency) {
        Formatting color;
        if (latency <= 0) {
             return Text.literal("...").formatted(Formatting.GRAY);
        } else if (latency < 100) {
            color = Formatting.GREEN;
        } else if (latency < 200) {
            color = Formatting.YELLOW;
        } else {
            color = Formatting.RED;
        }
        return Text.literal(latency + " ⏳").formatted(color);
    }
    
    private void registerTickBasedReaders() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                if (!lastPlayerMap.isEmpty()) {
                    lastPlayerMap.clear();
                }
                return;
            }

        if (client.world.getTime() % 20 != 0) return;
        
        if (moduleJoinLeave) {
            Map<UUID, String> currentPlayerMap = client.getNetworkHandler().getPlayerList().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getProfile().getId(),
                            entry -> entry.getProfile().getName()
                    ));

            if (lastPlayerMap.isEmpty() && !currentPlayerMap.isEmpty()) {
                lastPlayerMap.putAll(currentPlayerMap);
                return;
            }

            for (Map.Entry<UUID, String> entry : currentPlayerMap.entrySet()) {
                if (!lastPlayerMap.containsKey(entry.getKey())) {
                    boolean skip = false;
                    if (!lastMessages.isEmpty()) {
                        for (Map.Entry<String, Integer> msg : lastMessages.entrySet()) {
                            if (msg.getKey().contains(entry.getValue()))
                            {
                                skip = true;
                                break;
                            }
                        }
                    }

                    if (!skip) {
                        MutableText message = Text.literal("+ ").formatted(Formatting.AQUA)
                            .append(Text.literal(entry.getValue()).formatted(Formatting.GREEN));
                        client.player.sendMessage(message, false);
                    }
                }
            }

            for (Map.Entry<UUID, String> entry : lastPlayerMap.entrySet()) {
                if (!currentPlayerMap.containsKey(entry.getKey())) {
                    boolean skip = false;
                    if (!lastMessages.isEmpty()) {
                        for (Map.Entry<String, Integer> msg : lastMessages.entrySet()) {
                            if (msg.getKey().contains(entry.getValue()))
                            {
                                skip = true;
                                break;
                            }
                        }
                    }

                    if (!skip) {
                        MutableText message = Text.literal("- ").formatted(Formatting.AQUA)
                            .append(Text.literal(entry.getValue()).formatted(Formatting.RED));
                        client.player.sendMessage(message, false);
                    }
                }
            }

            lastPlayerMap = currentPlayerMap;

            if (!lastMessages.isEmpty()) {
                Iterator<Map.Entry<String, Integer>> iterator = lastMessages.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> msg = iterator.next();
                    int newTime = msg.getValue() - 1;

                    if (newTime <= 0) {
                        iterator.remove();
                    } else {
                        msg.setValue(newTime);
                    }
                }
            }
        }
        
        if (moduleGameMode) {
            Map<String, String> currentGameModeMap = client.getNetworkHandler().getPlayerList().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getProfile().getName(), 
                        entry -> entry.getGameMode().asString()
                ));
        
            if (lastGameModeMap.isEmpty() && !currentGameModeMap.isEmpty()) {
                lastGameModeMap.putAll(currentGameModeMap);
                return;
            }

            for (Map.Entry<String, String> entry : currentGameModeMap.entrySet()) {
                if (lastGameModeMap.get(entry.getKey()) == null) {
                    continue;
                }
                String lastGameMode = lastGameModeMap.get(entry.getKey());
                if (!entry.getValue().equals(lastGameMode)) {
                    MutableText message = Text.literal("🔔 ").formatted(Formatting.AQUA);
                    message.append(Text.literal(entry.getKey()).formatted(Formatting.GREEN));
                    message.append(Text.literal(" | ").formatted(Formatting.GRAY));
                    message.append(Text.literal(lastGameMode).formatted(Formatting.GOLD));
                    message.append(Text.literal(" → ").formatted(Formatting.WHITE));
                    message.append(Text.literal(entry.getValue()).formatted(Formatting.YELLOW));
                    client.player.sendMessage(message, false);
                }   
            }

            lastGameModeMap = currentGameModeMap;
        }
        
        if (moduleNear) {
            List<AbstractClientPlayerEntity> NearPlayers = client.world.getPlayers();
            if (LastNearPlayers != NearPlayers.size()) {
                if (NearPlayers.size() > 1) {
                    MutableText message = Text.literal("🔔 ").formatted(Formatting.AQUA);
                    message.append(Text.literal(String.valueOf(NearPlayers.size())).formatted(Formatting.RED));
                    message.append(Text.literal(" |").formatted(Formatting.GRAY));
                    for (AbstractClientPlayerEntity ACPE : NearPlayers) {
                        message.append(Text.literal(" " + ACPE.getGameProfile().getName()).formatted(Formatting.GRAY));
                    }
                    client.player.sendMessage(message, false);
                }

                else {
                    MutableText message = Text.literal("🔔 ").formatted(Formatting.AQUA);
                    message.append(Text.literal(String.valueOf(NearPlayers.size())).formatted(Formatting.GREEN));
                    message.append(Text.literal(" |").formatted(Formatting.GRAY));
                    for (AbstractClientPlayerEntity ACPE : NearPlayers) {
                        message.append(Text.literal(" " + ACPE.getGameProfile().getName()).formatted(Formatting.GRAY));
                    }
                    client.player.sendMessage(message, false);
                }
                LastNearPlayers = NearPlayers.size();
            }
        }
        
        
        });
    } 
}