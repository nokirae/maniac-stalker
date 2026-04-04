package com.github.nokirae.maniacstalker;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class Commands {
    public static void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ms")
                    .then(literal("ping")
                            .executes(context -> {
                                final MinecraftClient client = context.getSource().getClient();
                                if (client.player == null || client.getNetworkHandler() == null) {
                                    context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                    return 0;
                                }

                                Collection<PlayerListEntry> players = client.getNetworkHandler().getPlayerList();
                                if (players.isEmpty()) {
                                    context.getSource().sendFeedback(Funky.reColor(Text.translatable("noPlayers")));
                                    return 1;
                                }

                                List<PlayerListEntry> sortedPlayers = new ArrayList<>(players);
                                sortedPlayers.sort(Comparator.comparingInt(PlayerListEntry::getLatency));

                                MutableText message = Funky.reColor(Text.translatable("pingList"));
                                for (PlayerListEntry playerEntry : sortedPlayers) {
                                    message.append(Funky.reColor(Text.translatable("pingListPlayer", playerEntry.getProfile().name(), Funky.formatPing(playerEntry.getLatency()))));
                                }

                                context.getSource().sendFeedback(message);
                                return 1;
                            })
                            .then(argument("player", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String playerName = StringArgumentType.getString(context, "player");
                                        final MinecraftClient client = context.getSource().getClient();

                                        if (client.player == null || client.getNetworkHandler() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                            return 0;
                                        }

                                        PlayerListEntry targetPlayer = client.getNetworkHandler().getPlayerList().stream()
                                                .filter(p -> p.getProfile().name().equalsIgnoreCase(playerName))
                                                .findFirst()
                                                .orElse(null);

                                        if (targetPlayer != null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("pingPlayerSuccess", playerName, Funky.formatPing(targetPlayer.getLatency()))));
                                        } else {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("pingPlayerNotFound", playerName)));
                                        }
                                        return 1;
                                    }))
                    )
                    .then(literal("check").executes(context -> {
                        final MinecraftClient client = context.getSource().getClient();

                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                            return 0;
                        }

                        int tabCount = client.getNetworkHandler().getPlayerList().size();
                        int worldCount = client.world.getPlayers().size();

                        MutableText message = Funky.reColor(Text.translatable("check", tabCount, worldCount));

                        if (tabCount == worldCount) {
                            message.append(Funky.reColor(Text.translatable("checkPos")));
                        } else {
                            message.append(Funky.reColor(Text.translatable("checkNeg")));
                        }

                        context.getSource().sendFeedback(message);
                        return 1;
                    }))
                    .then(literal("near").executes(context -> {
                        final MinecraftClient client = context.getSource().getClient();

                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                            return 0;
                        }

                        List<AbstractClientPlayerEntity> Near = client.world.getPlayers();

                        for (AbstractClientPlayerEntity ACPE : Near) {
                            client.player.sendMessage(Funky.reColor(Text.translatable("nearListPlayer", ACPE.getGameProfile().name())), false);
                            /*
                            if (Variables.modulePowerNear) {
                                client.player.sendMessage(Text.of("Age: " + ACPE.age + ", DeathTime: " + ACPE.deathTime + ", Dist. Traveled: " + ACPE.distanceTraveled + ", Exp: " + ACPE.totalExperience), false);
                            } */
                        }

                        return 1;
                    }))
                    .then(literal("list").executes(context -> {
                        final MinecraftClient client = context.getSource().getClient();

                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                            return 0;
                        }

                        Collection<PlayerListEntry> Players = client.getNetworkHandler().getPlayerList();

                        for (PlayerListEntry PLE : Players) {
                            MutableText message = Funky.reColor(Text.translatable("listList"));
                            message.append(Funky.reColor(Text.translatable("listListPlayer", PLE.getProfile().name(), PLE.getGameMode().asString(), Funky.formatPing(PLE.getLatency()), PLE.getSkinTextures().model().name(), PLE.getProfile().id().toString())));

                            client.player.sendMessage(message, false);
                        }

                        return 1;
                    }))
                    .then(literal("toggle").executes(context -> {
                                        final MinecraftClient client = context.getSource().getClient();

                                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleJoinLeave = !Variables.moduleJoinLeave;
                                        Variables.moduleGameMode = !Variables.moduleGameMode;
                                        Variables.moduleNear = !Variables.moduleNear;

                                        MutableText message = Funky.reColor(Text.translatable("toggleDot"));

                                        if (Variables.moduleJoinLeave) {
                                            message.append(Funky.reColor(Text.translatable("toggleJoinLeaveOn")));
                                        } else {
                                            message.append(Funky.reColor(Text.translatable("toggleJoinLeaveOff")));
                                        }

                                        if (Variables.moduleGameMode) {
                                            message.append(Funky.reColor(Text.translatable("toggleGameModeOn")));
                                        } else {
                                            message.append(Funky.reColor(Text.translatable("toggleGameModeOff")));
                                        }

                                        if (Variables.moduleNear) {
                                            message.append(Funky.reColor(Text.translatable("toggleNearOn")));
                                        } else {
                                            message.append(Funky.reColor(Text.translatable("toggleNearOff")));
                                        }

                                        client.player.sendMessage(message, false);

                                        return 1;
                                    })
                                    .then(literal("JoinLeave").executes(context -> {
                                        final MinecraftClient client = context.getSource().getClient();

                                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleJoinLeave = !Variables.moduleJoinLeave;

                                        MutableText message = Funky.reColor(Text.translatable("toggleDot"));

                                        if (Variables.moduleJoinLeave) {
                                            message.append(Funky.reColor(Text.translatable("toggleJoinLeaveOn")));
                                        } else {
                                            message.append(Funky.reColor(Text.translatable("toggleJoinLeaveOff")));
                                        }

                                        client.player.sendMessage(message, false);

                                        return 1;
                                    }))
                                    .then(literal("GameMode").executes(context -> {
                                        final MinecraftClient client = context.getSource().getClient();

                                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleGameMode = !Variables.moduleGameMode;

                                        MutableText message = Funky.reColor(Text.translatable("toggleDot"));

                                        if (Variables.moduleGameMode) {
                                            message.append(Funky.reColor(Text.translatable("toggleGameModeOn")));
                                        } else {
                                            message.append(Funky.reColor(Text.translatable("toggleGameModeOff")));
                                        }

                                        client.player.sendMessage(message, false);

                                        return 1;
                                    }))
                                    .then(literal("Near").executes(context -> {
                                        final MinecraftClient client = context.getSource().getClient();

                                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.moduleNear = !Variables.moduleNear;

                                        MutableText message = Funky.reColor(Text.translatable("toggleDot"));

                                        if (Variables.moduleNear) {
                                            message.append(Funky.reColor(Text.translatable("toggleNearOn")));
                                        } else {
                                            message.append(Funky.reColor(Text.translatable("toggleNearOff")));
                                        }

                                        client.player.sendMessage(message, false);

                                        return 1;
                                    }))
                    )
                    .then(literal("uwu").executes(context -> {
                        final MinecraftClient client = context.getSource().getClient();

                        if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                            return 0;
                        }

                        Collection<PlayerListEntry> Players = client.getNetworkHandler().getPlayerList();

                        for (PlayerListEntry PLE : Players) {
                            MutableText message = Funky.reColor(Text.translatable("uwuListPlayer", PLE.getProfile().name()));
                            Text displayNameComponent = PLE.getDisplayName();
                            String fullRowText;

                            if (displayNameComponent != null) {
                                fullRowText = displayNameComponent.getString();
                            } else {
                                fullRowText = PLE.getProfile().name();
                            }

                            if (fullRowText.contains("♂")) {
                                if (PLE.getSkinTextures().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Text.translatable("uwuFemboyMaterial")));
                                } else {
                                    message.append(Funky.reColor(Text.translatable("uwuBoy")));
                                }
                            } else if (fullRowText.contains("♀")) {
                                if (PLE.getSkinTextures().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Text.translatable("uwuGirl")));
                                } else {
                                    message.append(Funky.reColor(Text.translatable("uwuTomboyMaterial")));
                                }
                            } else if (fullRowText.contains("⚥")) {
                                if (PLE.getSkinTextures().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Text.translatable("uwuFemboy")));
                                } else {
                                    message.append(Funky.reColor(Text.translatable("uwuTomboy")));
                                }
                            } else {
                                if (PLE.getSkinTextures().model().name().contains("SLIM")) {
                                    message.append(Funky.reColor(Text.translatable("uwuMaybeGirl")));
                                } else {
                                    message.append(Funky.reColor(Text.translatable("uwuMaybeBoy")));
                                }
                            }

                            client.player.sendMessage(message, false);
                        }

                        return 1;
                    }))

                    .then(literal("brand")
                            .executes(context -> {
                                final MinecraftClient client = context.getSource().getClient();
                                if (client.player == null || client.getNetworkHandler() == null) {
                                    context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                    return 0;
                                }

                                context.getSource().sendFeedback(Funky.reColor(Text.translatable("brandCurrent", Variables.customBrand)));
                                return 1;
                            })
                            .then(argument("newBrand", StringArgumentType.greedyString())
                                    .executes(context -> {
                                        String newBrand = StringArgumentType.getString(context, "newBrand");
                                        final MinecraftClient client = context.getSource().getClient();

                                        if (client.player == null || client.getNetworkHandler() == null) {
                                            context.getSource().sendFeedback(Funky.reColor(Text.translatable("notConnected")));
                                            return 0;
                                        }

                                        Variables.customBrand = newBrand;

                                        context.getSource().sendFeedback(Funky.reColor(Text.translatable("brandChanged", Variables.customBrand)));
                                        return 1;
                                    }))
                    )
            );
        });
    }
}
