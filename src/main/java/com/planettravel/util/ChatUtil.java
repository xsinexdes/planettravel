package com.planettravel.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

/**
 * Utilidades de mensajes.
 *
 * El actionbar se envia por la via de Spigot (spigot().sendMessage con
 * ChatMessageType.ACTION_BAR) en vez de Player#sendActionBar(String),
 * porque ese metodo esta deprecado en las versiones modernas de Paper
 * y podria desaparecer. Esta forma es estable en todas las versiones.
 */
public final class ChatUtil {

    private ChatUtil() {
    }

    public static void actionBar(Player jugador, String mensaje) {
        jugador.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(mensaje));
    }
}
