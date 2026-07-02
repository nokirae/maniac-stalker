package com.github.nokirae.maniacstalker;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
//import net.minecraft.text.MutableText;
//import net.minecraft.text.Text;
//import net.minecraft.util.Formatting;

public class Funky {
    public static Component formatPing(int latency) {
        String color;
        if (latency <= 0) {
            return Component.literal("\u00A77...");
        } else if (latency < 100) {
            color = "\u00A7a";
        } else if (latency < 200) {
            color = "\u00A7e";
        } else {
            color = "\u00A74";
        }
        return Component.literal(color + latency + " \u00A7r⏳");
    }

    public static MutableComponent reColor(MutableComponent text) {
        String string = text.getString();
        string = string.replace("&", "\u00A7");
        return Component.literal(string);
    }
}
