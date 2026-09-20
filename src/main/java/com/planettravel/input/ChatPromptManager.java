package com.planettravel.input;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Permite pedir un texto por chat desde una GUI: el jugador hace clic en un
 * boton, el menu se cierra, escribe el texto en el chat y se ejecuta la accion
 * con lo escrito. Escribir "cancelar" aborta.
 *
 * El mensaje escrito NO llega al chat publico (se cancela el evento), y la
 * accion se ejecuta en el hilo principal porque el chat de Bukkit es asincrono.
 */
public class ChatPromptManager implements Listener {

    private final JavaPlugin plugin;
    private final Map<UUID, Consumer<String>> pendientes = new ConcurrentHashMap<>();

    public ChatPromptManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void pedir(Player jugador, String pregunta, Consumer<String> accion) {
        pendientes.put(jugador.getUniqueId(), accion);
        plugin.getServer().getScheduler().runTask(plugin, jugador::closeInventory);
        jugador.sendMessage("§8§m                                        ");
        jugador.sendMessage("§e✎ " + pregunta);
        jugador.sendMessage("§7Escribe el texto en el chat. §8(Escribe §fcancelar §8para abortar)");
        jugador.sendMessage("§8§m                                        ");
    }

    public boolean estaEsperando(Player jugador) {
        return pendientes.containsKey(jugador.getUniqueId());
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Consumer<String> accion = pendientes.remove(event.getPlayer().getUniqueId());
        if (accion == null) {
            return;
        }

        event.setCancelled(true);
        String texto = event.getMessage();
        Player jugador = event.getPlayer();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (texto.equalsIgnoreCase("cancelar")) {
                jugador.sendMessage("§7Cancelado.");
                return;
            }
            accion.accept(texto);
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pendientes.remove(event.getPlayer().getUniqueId());
    }
}
