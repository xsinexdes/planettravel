package com.planettravel.selection;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Clics del hacha de seleccion: izquierdo = pos 1, derecho = pos 2. */
public class WandListener implements Listener {

    private final SelectionManager seleccion;

    public WandListener(SelectionManager seleccion) {
        this.seleccion = seleccion;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        if (!seleccion.esHacha(event.getItem())) {
            return;
        }

        Player jugador = event.getPlayer();
        if (!jugador.hasPermission("planettravel.admin")) {
            return;
        }
        if (event.getClickedBlock() == null) {
            return;
        }

        Action accion = event.getAction();
        Location punto = event.getClickedBlock().getLocation();

        if (accion == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            seleccion.setPos1(jugador, punto);
            informar(jugador, "1", punto);
        } else if (accion == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            seleccion.setPos2(jugador, punto);
            informar(jugador, "2", punto);
        }
    }

    private void informar(Player jugador, String numero, Location punto) {
        String texto = "§b✔ Posicion " + numero + " §7= §f" + punto.getBlockX() + ", "
                + punto.getBlockY() + ", " + punto.getBlockZ();

        Seleccion s = seleccion.getSeleccion(jugador);
        if (s != null) {
            texto += " §8(" + s.resumen() + ")";
        }
        jugador.sendMessage(texto);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        seleccion.limpiar(event.getPlayer().getUniqueId());
    }
}
