package com.github.nokirae.maniacstalker;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class Funky {
    public static Text formatPing(int latency) {
        String color;
        if (latency <= 0) {
            return Text.literal("\u00A77...");
        } else if (latency < 100) {
            color = "\u00A7a";
        } else if (latency < 200) {
            color = "\u00A7e";
        } else {
            color = "\u00A74";
        }
        return Text.literal(color + latency + " \u00A7r⏳");
    }

    public static MutableText reColor(MutableText text) {
        String string = text.getString();
        string = string.replace("&", "\u00A7");
        return Text.literal(string);
    }
}
