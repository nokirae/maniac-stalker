package com.github.nokirae.maniacstalker;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;

//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.network.AbstractClientPlayerEntity;
//import net.minecraft.client.network.PlayerListEntry;
//import net.minecraft.text.MutableText;
//import net.minecraft.text.Text;

import java.util.*;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class Commands {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ms")
                    .then(literal("ping")
                            .executes(context -> {
                                final Minecraft client = context.getSource().getClient();
                                if (client.player == null || client.getConnection() == null) {
                                    context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                    return 0;
                                }

                                Collection<PlayerInfo> players = client.getConnection().getOnlinePlayers();
                                if (players.isEmpty()) {
                                    context.getSource().sendFeedback(Funky.reColor(Component.translatable("noPlayers")));
                                    return 1;
                                }

                                List<PlayerInfo> sortedPlayers = new ArrayList<>(players);
                                sortedPlayers.sort(Comparator.comparingInt(PlayerInfo::getLatency));

                                MutableComponent message = Funky.reColor(Component.translatable("pingList"));
                                for (PlayerInfo playerEntry : sortedPlayers) {
                                    message.append(Funky.reColor(Component.translatable("pingListPlayer", playerEntry.getProfile().name(), Funky.formatPing(playerEntry.getLatency()))));
                                }

                                context.getSource().sendFeedback(message);
                                return 1;
                            })
                            .then(argument("player", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String playerName = StringArgumentType.getString(context, "player");
                                        final Minecraft client = context.getSource().getClient();

                                        if (client.player == null || client.getConnection() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                            return 0;
                                        }

                                        PlayerInfo targetPlayer = client.getConnection().getOnlinePlayers().stream()
                                                .filter(p -> p.getProfile().name().equalsIgnoreCase(playerName))
                                                .findFirst()
                                                .orElse(null);

                                        if (targetPlayer != null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("pingPlayerSuccess", playerName, Funky.formatPing(targetPlayer.getLatency()))));
                                        } else {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("pingPlayerNotFound", playerName)));
                                        }
                                        return 1;
                                    }))
                    )
                    .then(literal("check").executes(context -> {
                        final Minecraft client = context.getSource().getClient();

                        if (client.player == null || client.level == null || client.getConnection() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                            return 0;
                        }

                        int tabCount = client.getConnection().getOnlinePlayers().size();
                        int worldCount = client.level.players().size();

                        MutableComponent message = Funky.reColor(Component.translatable("check", tabCount, worldCount));

                        if (tabCount == worldCount) {
                            message.append(Funky.reColor(Component.translatable("checkPos")));
                        } else {
                            message.append(Funky.reColor(Component.translatable("checkNeg")));
                        }

                        context.getSource().sendFeedback(message);
                        return 1;
                    }))
                    .then(literal("near").executes(context -> {
                        final Minecraft client = context.getSource().getClient();

                        if (client.player == null || client.level == null || client.getConnection() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                            return 0;
                        }

                        List<AbstractClientPlayer> Near = client.level.players();

                        for (AbstractClientPlayer ACP : Near) {
                            client.player.sendSystemMessage(Funky.reColor(Component.translatable("nearListPlayer", ACP.getGameProfile().name())));
                            /*
                            if (Variables.modulePowerNear) {
                                client.player.sendMessage(Text.of("Age: " + ACPE.age + ", DeathTime: " + ACPE.deathTime + ", Dist. Traveled: " + ACPE.distanceTraveled + ", Exp: " + ACPE.totalExperience), false);
                            } */
                        }

                        return 1;
                    }))
                    .then(literal("list").executes(context -> {
                        final Minecraft client = context.getSource().getClient();

                        if (client.player == null || client.level == null || client.getConnection() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                            return 0;
                        }

                        Collection<PlayerInfo> Players = client.getConnection().getOnlinePlayers();

                        context.getSource().sendFeedback(Funky.reColor(Component.translatable("listList")));

                        for (PlayerInfo PI : Players) {
                            // context.getSource().sendFeedback(Funky.reColor(Component.translatable("listListPlayer", PI.getProfile().name(), PI.getGameMode().toString(), Funky.formatPing(PI.getLatency()), PI.getSkin().model().name(), PI.getProfile().id().toString())));
                            UUID playerUuid = PI.getProfile().id();
                            Component uuidComponent = Component.literal(playerUuid.toString())
                                    .withStyle(style -> style
                                            .withClickEvent(new ClickEvent.CopyToClipboard(playerUuid.toString()))
                                            .withHoverEvent(new HoverEvent.ShowText(Funky.reColor(Component.translatable("listCopyUUID"))))
                                            .withUnderlined(true)
                                    );

                            Component message = Funky.reColor(Component.translatable("listListPlayer", PI.getProfile().name(), PI.getGameMode().toString(), Funky.formatPing(PI.getLatency()), PI.getSkin().model().name()))
                                .append(" ")
                                .append(uuidComponent);

                            client.player.sendSystemMessage(message);
                        }

                        return 1;
                    }))
                    .then(literal("toggle").executes(context -> {
                                        final Minecraft client = context.getSource().getClient();

                                        if (client.player == null || client.level == null || client.getConnection() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleJoinLeave = !Variables.moduleJoinLeave;
                                        Variables.moduleGameMode = !Variables.moduleGameMode;
                                        Variables.moduleNear = !Variables.moduleNear;

                                        MutableComponent message = Funky.reColor(Component.translatable("toggleDot"));

                                        if (Variables.moduleJoinLeave) {
                                            message.append(Funky.reColor(Component.translatable("toggleJoinLeaveOn")));
                                        } else {
                                            message.append(Funky.reColor(Component.translatable("toggleJoinLeaveOff")));
                                        }

                                        if (Variables.moduleGameMode) {
                                            message.append(Funky.reColor(Component.translatable("toggleGameModeOn")));
                                        } else {
                                            message.append(Funky.reColor(Component.translatable("toggleGameModeOff")));
                                        }

                                        if (Variables.moduleNear) {
                                            message.append(Funky.reColor(Component.translatable("toggleNearOn")));
                                        } else {
                                            message.append(Funky.reColor(Component.translatable("toggleNearOff")));
                                        }

                                        client.player.sendSystemMessage(message);

                                        return 1;
                                    })
                                    .then(literal("JoinLeave").executes(context -> {
                                        final Minecraft client = context.getSource().getClient();

                                        if (client.player == null || client.level == null || client.getConnection() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleJoinLeave = !Variables.moduleJoinLeave;

                                        MutableComponent message = Funky.reColor(Component.translatable("toggleDot"));

                                        if (Variables.moduleJoinLeave) {
                                            message.append(Funky.reColor(Component.translatable("toggleJoinLeaveOn")));
                                        } else {
                                            message.append(Funky.reColor(Component.translatable("toggleJoinLeaveOff")));
                                        }

                                        client.player.sendSystemMessage(message);

                                        return 1;
                                    }))
                                    .then(literal("GameMode").executes(context -> {
                                        final Minecraft client = context.getSource().getClient();

                                        if (client.player == null || client.level == null || client.getConnection() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleGameMode = !Variables.moduleGameMode;

                                        MutableComponent message = Funky.reColor(Component.translatable("toggleDot"));

                                        if (Variables.moduleGameMode) {
                                            message.append(Funky.reColor(Component.translatable("toggleGameModeOn")));
                                        } else {
                                            message.append(Funky.reColor(Component.translatable("toggleGameModeOff")));
                                        }

                                        client.player.sendSystemMessage(message);

                                        return 1;
                                    }))
                                    .then(literal("Near").executes(context -> {
                                        final Minecraft client = context.getSource().getClient();

                                        if (client.player == null || client.level == null || client.getConnection() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleNear = !Variables.moduleNear;

                                        MutableComponent message = Funky.reColor(Component.translatable("toggleDot"));

                                        if (Variables.moduleNear) {
                                            message.append(Funky.reColor(Component.translatable("toggleNearOn")));
                                        } else {
                                            message.append(Funky.reColor(Component.translatable("toggleNearOff")));
                                        }

                                        client.player.sendSystemMessage(message);

                                        return 1;
                                    }))
                    )
                    .then(literal("uwu").executes(context -> {
                        final Minecraft client = context.getSource().getClient();

                        if (client.player == null || client.level == null || client.getConnection() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                            return 0;
                        }

                        Collection<PlayerInfo> Players = client.getConnection().getOnlinePlayers();

                        for (PlayerInfo PI : Players) {
                            MutableComponent message = Funky.reColor(Component.translatable("uwuListPlayer", PI.getProfile().name()));
                            Component displayNameComponent = PI.getTabListDisplayName();
                            String fullRowText;

                            if (displayNameComponent != null) {
                                fullRowText = displayNameComponent.getString();
                            } else {
                                fullRowText = PI.getProfile().name();
                            }

                            if (fullRowText.contains("♂")) {
                                if (PI.getSkin().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Component.translatable("uwuFemboyMaterial")));
                                } else {
                                    message.append(Funky.reColor(Component.translatable("uwuBoy")));
                                }
                            } else if (fullRowText.contains("♀")) {
                                if (PI.getSkin().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Component.translatable("uwuGirl")));
                                } else {
                                    message.append(Funky.reColor(Component.translatable("uwuTomboyMaterial")));
                                }
                            } else if (fullRowText.contains("⚥")) {
                                if (PI.getSkin().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Component.translatable("uwuFemboy")));
                                } else {
                                    message.append(Funky.reColor(Component.translatable("uwuTomboy")));
                                }
                            } else {
                                if (PI.getSkin().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Component.translatable("uwuMaybeGirl")));
                                } else {
                                    message.append(Funky.reColor(Component.translatable("uwuMaybeBoy")));
                                }
                            }

                            client.player.sendSystemMessage(message);
                        }

                        return 1;
                    }))

                    .then(literal("brand")
                            .executes(context -> {
                                final Minecraft client = context.getSource().getClient();
                                if (client.player == null || client.getConnection() == null) {
                                    context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                    return 0;
                                }

                                context.getSource().sendFeedback(Funky.reColor(Component.translatable("brandCurrent", Variables.customBrand)));
                                return 1;
                            })
                            .then(argument("newBrand", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String newBrand = StringArgumentType.getString(context, "newBrand");
                                        final Minecraft client = context.getSource().getClient();

                                        if (client.player == null || client.getConnection() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Component.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.customBrand = newBrand;

                                        context.getSource().sendFeedback(Funky.reColor(Component.translatable("brandChanged", Variables.customBrand)));
                                        return 1;
                                    }))
                    )
            );
        });
    }
}
