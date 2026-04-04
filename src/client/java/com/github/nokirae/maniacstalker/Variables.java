package com.github.nokirae.maniacstalker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Variables {
    public static Map<UUID, String> lastPlayerMap = new HashMap<>();
    public static Map<String, String> lastGameModeMap = new HashMap<>();
    public static Map<String, Integer> lastMessages = new HashMap<>();

    public static int lastNearPlayers = 1;
    public static int messagesTimeOut = 5; // seconds

    public static boolean moduleJoinLeave = true;
    public static boolean moduleGameMode = true;
    public static boolean moduleNear = true;
    // public static boolean modulePowerNear = true;

    public static String customBrand = "fabric";
}
