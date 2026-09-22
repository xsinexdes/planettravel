package com.planettravel.selection;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.SpaceSettings;
import com.planettravel.selection.SelectionManager.ModoPunto;
import com.planettravel.selection.SelectionManager.TipoPunto;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Clics del hacha de seleccion.
 *
 * Modo normal: izquierdo = pos 1, derecho = pos 2 (area, igual que WorldEdit).
 *
 * Modo "marcar punto" (activado desde /planeta hacha entrada|deteccion <id> o
 * desde el editor): clic izquierdo = esquina A, clic derecho = esquina B, y
 * asi se marca la ZONA de entrada al planeta o la caja de deteccion opcional
 * en el espacio (o el sol, con un solo clic). Este es el unico sitio del
 * plugin donde se marcan esos puntos con precision de bloque.
 *
 * La SALIDA (despegue) ya no se marca: se calcula sola a partir del radio de
 * deteccion del planeta (ver {@link PlanetData#puntoSalidaAutomatico}).
 */
public class WandListener implements Listener {

    private final SelectionManager seleccion;
    private final PlanetConfigManager config;

    public WandListener(SelectionManager seleccion, PlanetConfigManager config) {
        this.seleccion = seleccion;
        this.config = config;
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

        ModoPunto modo = seleccion.getModoPunto(jugador);
        if (modo != null) {
            if (accion == Action.LEFT_CLICK_BLOCK || accion == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                manejarSeleccionPunto(jugador, modo, punto, accion == Action.LEFT_CLICK_BLOCK);
            }
            return;
        }

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

    // ------------------------------------------------------------
    //  Modo "marcar punto": entrada / salida / sol
    // ------------------------------------------------------------

    private void manejarSeleccionPunto(Player jugador, ModoPunto modo, Location punto, boolean esClicIzquierdo) {
        if (modo.tipo() == TipoPunto.SOL) {
            marcarSol(jugador, punto);
            return;
        }

        PlanetData planeta = config.getPlaneta(modo.idPlaneta()).orElse(null);
        if (planeta == null) {
            jugador.sendMessage("§cEse planeta ya no existe. Cancelado.");
            seleccion.limpiarModoPunto(jugador.getUniqueId());
            return;
        }

        boolean esEntrada = modo.tipo() == TipoPunto.ENTRADA;
        boolean esDeteccion = modo.tipo() == TipoPunto.DETECCION;
        // La entrada se marca en el mundo del planeta; la caja de deteccion,
        // en el espacio (la salida ya no se marca: es automatica).
        String nombreMundoEsperado = esEntrada ? planeta.getWorldName() : config.getMundoEspacio();
        World mundoEsperado = Bukkit.getWorld(nombreMundoEsperado);

        if (mundoEsperado == null || punto.getWorld() == null || !punto.getWorld().equals(mundoEsperado)) {
            jugador.sendMessage("§cDebes marcar este punto §7dentro de§c: §f" + nombreMundoEsperado
                    + " §7(estas en §f" + (punto.getWorld() == null ? "?" : punto.getWorld().getName()) + "§7). "
                    + "Ve alli y vuelve a hacer clic con el hacha.");
            return; // no se cancela el modo: puede moverse y volver a intentarlo
        }

        Location destino = punto.clone().add(0.5, 1.0, 0.5);
        destino.setYaw(jugador.getLocation().getYaw());
        destino.setPitch(jugador.getLocation().getPitch());

        // Igual que la seleccion de area normal: clic izquierdo = esquina A,
        // clic derecho = esquina B. Con solo la esquina A ya funciona (zona de
        // un punto); marcar tambien la B convierte eso en una ZONA de verdad,
        // que es la region donde se elegira un punto al azar en cada viaje -
        // asi el "planeta fisico" se siente real y las naves no colisionan
        // siempre en el mismo sitio exacto.
        if (esDeteccion) {
            if (esClicIzquierdo) {
                planeta.setDeteccionCaja(destino.getX(), destino.getY(), destino.getZ());
                jugador.sendMessage("§b✔ Esquina A de la caja de DETECCION de '" + planeta.getNombreVisible()
                        + "' marcada en §f" + destino.getBlockX() + ", " + destino.getBlockY() + ", " + destino.getBlockZ() + "§b.");
            } else {
                planeta.setDeteccionCajaEsquinaB(destino.getX(), destino.getY(), destino.getZ());
                jugador.sendMessage("§b✔ Esquina B de la caja de DETECCION marcada. §7Caja: §f"
                        + (int) planeta.getDeteccionCajaAnchoX() + " x " + (int) planeta.getDeteccionCajaAnchoY()
                        + " x " + (int) planeta.getDeteccionCajaAnchoZ() + " §7bloques.");
            }
            planeta.setDeteccionCajaLista(true);
            if (!planeta.isDeteccionUsaCaja()) {
                jugador.sendMessage("§7Recuerda activarla con: §f/planeta editar " + planeta.getId() + " deteccioncaja");
            }
        } else {
            if (esClicIzquierdo) {
                planeta.setSpawnPlaneta(destino.getX(), destino.getY(), destino.getZ(), destino.getYaw(), destino.getPitch());
                jugador.sendMessage("§a✔ Esquina A de ENTRADA de '" + planeta.getNombreVisible() + "' marcada en §f"
                        + destino.getBlockX() + ", " + destino.getBlockY() + ", " + destino.getBlockZ() + "§a.");
            } else {
                planeta.setSpawnPlanetaEsquinaB(destino.getX(), destino.getY(), destino.getZ());
                jugador.sendMessage("§a✔ Esquina B de ENTRADA marcada. §7Zona: §f"
                        + (int) planeta.getEntradaZonaAnchoX() + " x " + (int) planeta.getEntradaZonaAnchoZ() + " §7bloques.");
            }
            planeta.setEntradaLista(true);
        }

        config.registrarPlaneta(planeta);
        config.guardarTodo();

        jugador.sendMessage("§7(Sigues en modo marcado: clic §fizquierdo§7=esquina A, clic §fderecho§7=esquina B. "
                + "§f/planeta hacha §7para salir de este modo.)");

        if (esEntrada && planeta.puedeViajar()) {
            jugador.sendMessage("§b✔ '" + planeta.getNombreVisible() + "' ya tiene entrada configurada: listo para viajar "
                    + "(la salida es automatica, no hace falta marcarla).");
        }
    }

    private void marcarSol(Player jugador, Location punto) {
        SpaceSettings espacio = config.getSpaceSettings();
        String mundoEspacio = config.getMundoEspacio();
        if (punto.getWorld() == null || !punto.getWorld().getName().equals(mundoEspacio)) {
            jugador.sendMessage("§cEl sol solo se puede marcar dentro del mundo espacio (§f" + mundoEspacio + "§c).");
            return;
        }
        espacio.setSolPosicion(punto.getX(), punto.getY(), punto.getZ());
        espacio.setSolActivo(true);
        config.guardarTodo();
        seleccion.limpiarModoPunto(jugador.getUniqueId());
        jugador.sendMessage("§6☀ Sol marcado en §f" + punto.getBlockX() + ", " + punto.getBlockY() + ", " + punto.getBlockZ()
                + "§6. §7Cuanto mas cerca de ese punto en el espacio, mas calor; cuanto mas lejos, mas frio.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        seleccion.limpiar(event.getPlayer().getUniqueId());
    }
}
