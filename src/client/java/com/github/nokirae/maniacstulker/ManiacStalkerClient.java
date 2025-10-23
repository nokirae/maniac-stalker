package com.github.nokirae.maniacstulker;

//import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.network.PlayerListEntry;
//import net.minecraft.scoreboard.ScoreboardObjective;
//import net.minecraft.scoreboard.ScoreHolder;
//import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import java.util.Collection;
//import net.minecraft.scoreboard.ScoreboardDisplaySlot;
//import net.minecraft.client.MinecraftClient;
//import net.minecraft.client.network.PlayerListEntry;
//import net.minecraft.text.MutableText;
//import net.minecraft.text.Text;
//import net.minecraft.util.Formatting;
//import java.util.Collection;
import java.util.HashMap;
//import java.util.HashSet;
import java.util.Map;
//import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
//import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
//import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
//import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collection;
//import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;
import net.minecraft.client.network.AbstractClientPlayerEntity;

public class ManiacStalkerClient implements ClientModInitializer {
        public boolean Debug = false;
    	public static final String MOD_ID = "maniac-stalker";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
        //Collection<PlayerListEntry> lastPlayers = null;
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
            // Реєструємо головну подію - підключення до сервера
            ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            // Цей код виконається один раз при вході на сервер
            LOGGER.info("Успішне підключення! Починаю винюхувати...");

            // Після підключення, починаємо слухати інші події
            registerChatReader();
            registerTickBasedReaders();
            registerPingCommand();
        });
    }
        
    /**
     * Реєструє слухача для читання повідомлень з чату.
     * Цей метод спрацьовує на кожне нове повідомлення.
     */
    
    private void registerChatReader() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            String chatMessage = message.getString();
            if (Debug){
                LOGGER.info("Чат Парсер | " + chatMessage);
            }
            lastMessages.put(chatMessage, messagesTimeOut);
            // Тут ви можете аналізувати повідомлення, шукати ключові слова тощо.
        });
    }
    
    
    private void registerPingCommand() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(literal("ms")
                // Реєструємо команду /ms ping, яка покаже пінг ВСІХ гравців
                .then(literal("ping")
                    .executes(context -> {
                        // Логіка для /ms ping (без аргументів)
                        final MinecraftClient client = context.getSource().getClient();
                        if (client.player == null || client.getNetworkHandler() == null) {
                            context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
                            return 0;
                        }

                        // Отримуємо список всіх гравців
                        Collection<PlayerListEntry> players = client.getNetworkHandler().getPlayerList();
                        if (players.isEmpty()) {
                            context.getSource().sendFeedback(Text.literal("На сервері немає гравців.").formatted(Formatting.YELLOW));
                            return 1;
                        }
                        
                        // Створюємо список для сортування
                        List<PlayerListEntry> sortedPlayers = new ArrayList<>(players);
                        sortedPlayers.sort(Comparator.comparingInt(PlayerListEntry::getLatency));

                        // Створюємо красиве повідомлення
                        MutableText fullMessage = Text.literal("--- Пінг гравців ---").formatted(Formatting.GOLD);
                        for (PlayerListEntry playerEntry : sortedPlayers) {
                            String name = playerEntry.getProfile().getName();
                            int latency = playerEntry.getLatency();
                            
                            // Додаємо новий рядок
                            fullMessage.append(Text.literal("\n" + name + ": ").formatted(Formatting.GRAY))
                                       .append(formatPing(latency));
                        }
                        
                        context.getSource().sendFeedback(fullMessage);
                        return 1;
                    })
                    // Додаємо можливість вказати нікнейм: /ms ping <нік>
                    .then(argument("player", StringArgumentType.greedyString())
                        .executes(context -> {
                            // Логіка для /ms ping <нікнейм>
                            String playerName = StringArgumentType.getString(context, "player");
                            final MinecraftClient client = context.getSource().getClient();

                            if (client.player == null || client.getNetworkHandler() == null) {
                                context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
                                return 0;
                            }

                            // Шукаємо гравця за ніком
                            PlayerListEntry targetPlayer = client.getNetworkHandler().getPlayerList().stream()
                                    .filter(p -> p.getProfile().getName().equalsIgnoreCase(playerName))
                                    .findFirst()
                                    .orElse(null);

                            if (targetPlayer != null) {
                                MutableText message = Text.literal("Пінг особи ").formatted(Formatting.AQUA)
                                        .append(Text.literal(playerName).formatted(Formatting.YELLOW))
                                        .append(Text.literal(": ").formatted(Formatting.AQUA))
                                        .append(formatPing(targetPlayer.getLatency()));
                                context.getSource().sendFeedback(message);
                            } else {
                                context.getSource().sendError(Text.literal("Особу з ніком '" + playerName + "' не знайдено."));
                            }
                            return 1;
                        }))
                )
                 // Додаємо нову під-команду "check"
                .then(literal("check").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
                        return 0;
                    }

                    // 1. Отримуємо кількість гравців з TAB-списку
                    int tabCount = client.getNetworkHandler().getPlayerList().size();

                    // 2. Отримуємо кількість завантажених сутностей-гравців у світі
                    int worldCount = client.world.getPlayers().size();

                    MutableText message = Text.literal("--- Перевірка осіб ---").formatted(Formatting.GOLD);

                    message.append(Text.literal("\nКількість у TAB-списку: ").formatted(Formatting.GRAY))
                           .append(Text.literal(String.valueOf(tabCount)).formatted(Formatting.YELLOW));

                    message.append(Text.literal("\nКількість у світі (промальовка): ").formatted(Formatting.GRAY))
                           .append(Text.literal(String.valueOf(worldCount)).formatted(Formatting.YELLOW));

                    // 3. Порівнюємо їх
                    if (tabCount == worldCount) {
                        message.append(Text.literal("\n\n ✅ Кількість осіб збігається.").formatted(Formatting.GREEN));
                    } else {
                        message.append(Text.literal("\n\n ⚠️ Увага! Кількість осіб не збігається.").formatted(Formatting.RED));
                        message.append(Text.literal("\nЦе може бути через розсинхронізацію або особливості сервера.").formatted(Formatting.GRAY));
                    }

                    context.getSource().sendFeedback(message);
                    return 1;
                }))
                 // Додаємо нову під-команду "near"
                .then(literal("near").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
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
                 // Додаємо нову під-команду "list"
                .then(literal("list").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
                        return 0;
                    }

                    Collection<PlayerListEntry> Players = client.getNetworkHandler().getPlayerList();
                    
                    for (PlayerListEntry PLE : Players) {
                        MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA);
                        message.append(Text.literal(PLE.getProfile().getName()).formatted(Formatting.GREEN));
                        message.append(Text.literal(" | "+ PLE.getGameMode().asString() + " | " + PLE.getLatency() + " мс.").formatted(Formatting.GRAY));
                        message.append(Text.literal("\n" + PLE.getProfile().getId().toString() + "\n").formatted(Formatting.GRAY));
                        client.player.sendMessage(message, false);
                    }

                    return 1;
                }))
                 // Додаємо нову під-команду "toggle"
                .then(literal("toggle").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
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
                // Додаємо можливість вказати нікнейм: /ms ping <нік>
                    .then(argument("module", StringArgumentType.greedyString())
                        .executes(context -> {
                            // Логіка для /ms ping <нікнейм>
                            String moduleName = StringArgumentType.getString(context, "module");
                            final MinecraftClient client = context.getSource().getClient();

                            if (client.player == null || client.getNetworkHandler() == null) {
                                context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
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
                 // Додаємо нову під-команду "uwu"
                .then(literal("uwu").executes(context -> {
                    final MinecraftClient client = context.getSource().getClient();

                    if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                        context.getSource().sendError(Text.literal("Ви не підключені до сервера."));
                        return 0;
                    }

                    Collection<PlayerListEntry> Players = client.getNetworkHandler().getPlayerList();
                    
                    for (PlayerListEntry PLE : Players) {
                        MutableText message = Text.literal("⏺ ").formatted(Formatting.AQUA);
                        message.append(Text.literal(PLE.getProfile().getName()).formatted(Formatting.GREEN));
                        message.append(Text.literal(" - ").formatted(Formatting.GRAY));
                        //message.append(Text.literal(PLE.getSkinTextures().model().getName()).formatted(Formatting.GOLD));
                        Text displayNameComponent = PLE.getDisplayName();
    
                        String fullRowText;

                        if (displayNameComponent != null) {
                            // Отримуємо повний рядок (наприклад, "[Admin] Steve_Z [Helper]")
                            fullRowText = displayNameComponent.getString();
                        } else {
                            // Запасний варіант, якщо displayName чомусь null
                            fullRowText = PLE.getProfile().getName();
                        }
                        
                        //message.append(Text.literal(fullRowText).formatted(Formatting.GOLD));
                        
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
        if (latency <= 0) { // < 0 означає, що дані ще не отримані
             return Text.literal("...").formatted(Formatting.GRAY);
        } else if (latency < 100) {
            color = Formatting.GREEN;
        } else if (latency < 200) {
            color = Formatting.YELLOW;
        } else {
            color = Formatting.RED;
        }
        return Text.literal(latency + " мс.").formatted(color);
    }
    /**
     * Реєструє слухача, який буде працювати кожен тік гри.
     * Використовується для читання даних, що постійно оновлюються, як-от TAB та скорборд.
     */
    private void registerTickBasedReaders() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null || client.getNetworkHandler() == null) {
                if (!lastPlayerMap.isEmpty()) {
                    lastPlayerMap.clear();
                }
                return;
            }

        if (client.world.getTime() % 20 != 0) return; // Перевірка раз на секунду
        
        if (moduleJoinLeave) {
            // 1. Отримуємо поточну мапу гравців (UUID -> Name)
            Map<UUID, String> currentPlayerMap = client.getNetworkHandler().getPlayerList().stream()
                    .collect(Collectors.toMap(
                            entry -> entry.getProfile().getId(),
                            entry -> entry.getProfile().getName()
                    ));

            // Пропускаємо першу перевірку
            if (lastPlayerMap.isEmpty() && !currentPlayerMap.isEmpty()) {
                lastPlayerMap.putAll(currentPlayerMap);
                return;
            }

            // 2. Гравці, що зайшли (є в новому списку, але не було в старому)
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

            // 3. Гравці, що вийшли (були в старому списку, але немає в новому)
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

            // 4. Оновлюємо стан
            lastPlayerMap = currentPlayerMap;

            if (!lastMessages.isEmpty()) {
                // Отримуємо ітератор для безпечного видалення
                Iterator<Map.Entry<String, Integer>> iterator = lastMessages.entrySet().iterator();

                while (iterator.hasNext()) {
                    Map.Entry<String, Integer> msg = iterator.next();
                    int newTime = msg.getValue() - 1; // Зменшуємо час "життя" повідомлення

                    if (newTime <= 0) {
                        // Час вийшов - безпечно видаляємо елемент через ітератор
                        iterator.remove();
                    } else {
                        // Час ще не вийшов - оновлюємо значення
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
                    /*
                    MutableText message = Text.literal("🔔 ").formatted(Formatting.AQUA);
                    message.append(Text.literal(entry.getKey()).formatted(Formatting.GREEN));
                    message.append(Text.literal(" | ").formatted(Formatting.GRAY));
                    message.append(Text.literal(entry.getValue()).formatted(Formatting.YELLOW));
                    client.player.sendMessage(message, false);
                    */
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
            //int NearPlayers = client.world.getPlayers().size();
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