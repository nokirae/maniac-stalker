package com.github.nokirae.maniacstalker;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

//import net.minecraft.client.network.AbstractClientPlayerEntity;
//import net.minecraft.text.MutableText;
//import net.minecraft.text.Text;

import java.util.*;
import java.util.stream.Collectors;

public class TickBase {
    public static void registerTickBasedReaders() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null || client.getConnection() == null) {
                if (!Variables.lastPlayerMap.isEmpty()) {
                    Variables.lastPlayerMap.clear();
                }
                return;
            }

            if (client.level.getGameTime() % 20 != 0) return;

            if (Variables.moduleJoinLeave) {
                Map<UUID, String> currentPlayerMap = client.getConnection().getOnlinePlayers().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getProfile().id(),
                                entry -> entry.getProfile().name()
                        ));

                if (Variables.lastPlayerMap.isEmpty() && !currentPlayerMap.isEmpty()) {
                    Variables.lastPlayerMap.putAll(currentPlayerMap);
                    return;
                }

                for (Map.Entry<UUID, String> entry : currentPlayerMap.entrySet()) {
                    if (!Variables.lastPlayerMap.containsKey(entry.getKey())) {
                        boolean skip = false;
                        if (!Variables.lastMessages.isEmpty()) {
                            for (Map.Entry<String, Integer> msg : Variables.lastMessages.entrySet()) {
                                if (msg.getKey().contains(entry.getValue()))
                                {
                                    skip = true;
                                    break;
                                }
                            }
                        }

                        if (!skip) {
                            client.player.sendSystemMessage(Funky.reColor(Component.translatable("silentJoin", entry.getValue())));
                        }
                    }
                }

                for (Map.Entry<UUID, String> entry : Variables.lastPlayerMap.entrySet()) {
                    if (!currentPlayerMap.containsKey(entry.getKey())) {
                        boolean skip = false;
                        if (!Variables.lastMessages.isEmpty()) {
                            for (Map.Entry<String, Integer> msg : Variables.lastMessages.entrySet()) {
                                if (msg.getKey().contains(entry.getValue()))
                                {
                                    skip = true;
                                    break;
                                }
                            }
                        }

                        if (!skip) {
                            client.player.sendSystemMessage(Funky.reColor(Component.translatable("silentLeave", entry.getValue())));
                        }
                    }
                }

                Variables.lastPlayerMap = new HashMap<>(currentPlayerMap);

                if (!Variables.lastMessages.isEmpty()) {
                    Iterator<Map.Entry<String, Integer>> iterator = Variables.lastMessages.entrySet().iterator();

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

            if (Variables.moduleGameMode) {
                Map<String, String> currentGameModeMap = client.getConnection().getOnlinePlayers().stream()
                        .collect(Collectors.toMap(
                                entry -> entry.getProfile().name(),
                                entry -> entry.getGameMode().toString(),
                                (existing, replacement) -> replacement
                        ));

                if (Variables.lastGameModeMap.isEmpty() && !currentGameModeMap.isEmpty()) {
                    Variables.lastGameModeMap.putAll(currentGameModeMap);
                    return;
                }

                for (Map.Entry<String, String> entry : currentGameModeMap.entrySet()) {
                    if (Variables.lastGameModeMap.get(entry.getKey()) == null) {
                        continue;
                    }
                    String lastGameMode = Variables.lastGameModeMap.get(entry.getKey());
                    if (!entry.getValue().equals(lastGameMode)) {
                        client.player.sendSystemMessage(Funky.reColor(Component.translatable("gameModeChanged", entry.getKey(), lastGameMode, entry.getValue())));
                    }
                }

                Variables.lastGameModeMap = currentGameModeMap;
            }

            if (Variables.moduleNear) {
                List<AbstractClientPlayer> NearPlayers = client.level.players();
                if (Variables.lastNearPlayers != NearPlayers.size()) {
                    MutableComponent message = Component.literal("");

                    if (NearPlayers.size() > 1) {
                        message.append(Funky.reColor(Component.translatable("nearSomebody", NearPlayers.size())));
                        for (AbstractClientPlayer ACP : NearPlayers) {
                            message.append(Component.literal(" " + ACP.getGameProfile().name()));
                            /*
                            if (Variables.modulePowerNear) {
                                message.append(Text.literal("Age: " + ACPE.age + ", DeathTime: " + ACPE.deathTime + ", Dist. Traveled: " + ACPE.distanceTraveled + ", Exp: " + ACPE.totalExperience));
                            } */
                        }
                    }

                    else {
                        message.append(Funky.reColor(Component.translatable("nearOnlyYou")));
                    }

                    client.player.sendSystemMessage(message);

                    Variables.lastNearPlayers = NearPlayers.size();
                }
            }
        });
    }
}
