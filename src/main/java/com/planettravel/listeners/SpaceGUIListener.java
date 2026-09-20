package com.planettravel.listeners;

import com.planettravel.PlanetTravel;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.config.SpaceSettings;
import com.planettravel.config.SuitTier;
import com.planettravel.gui.BasesGUI;
import com.planettravel.gui.SpaceMenuGUI;
import com.planettravel.gui.SuitGUI;
import com.planettravel.selection.Seleccion;
import com.planettravel.space.SpaceBase;
import com.planettravel.suit.SuitManager;
import com.planettravel.util.Textos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Clics de las GUIs nuevas: menu del espacio, bases espaciales y trajes.
 * Igual que en el resto de GUIs: clic izquierdo = subir/activar, derecho =
 * bajar/desactivar, Shift = paso grande. Siempre se cancela el evento.
 */
public class SpaceGUIListener implements Listener {

    private final PlanetTravel plugin;
    private final PlanetConfigManager config;

    public SpaceGUIListener(PlanetTravel plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();

        boolean menu = titulo.equals(SpaceMenuGUI.TITULO);
        boolean bases = titulo.equals(BasesGUI.TITULO);
        boolean listaTrajes = titulo.equals(SuitGUI.TITULO_LISTA);
        boolean tierTraje = titulo.startsWith(SuitGUI.PREFIJO_TIER);
        if (!menu && !bases && !listaTrajes && !tierTraje) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player jugador)) return;
        if (!jugador.hasPermission("planettravel.admin")) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) return;

        if (menu) {
            menuEspacio(event, jugador);
        } else if (bases) {
            menuBases(event, jugador);
        } else if (listaTrajes) {
            menuListaTrajes(event, jugador);
        } else {
            menuTier(event, jugador, SuitGUI.tierDesdeTitulo(titulo));
        }
    }

    // ============================================================
    //  MENU DEL ESPACIO
    // ============================================================

    private void menuEspacio(InventoryClickEvent event, Player jugador) {
        SpaceSettings s = config.getSpaceSettings();
        PlanetEnvironment env = s.getEntorno();

        ClickType tipo = event.getClick();
        boolean izq = tipo.isLeftClick();
        double paso = tipo.isShiftClick() ? 5.0 : 1.0;
        double signo = izq ? 1 : -1;

        switch (event.getRawSlot()) {
            case SpaceMenuGUI.SLOT_ACTIVO -> s.setActivo(!s.isActivo());
            case SpaceMenuGUI.SLOT_CASCO -> env.setRequiereCasco(!env.isRequiereCasco());
            case SpaceMenuGUI.SLOT_TIER -> {
                int maximo = Math.max(1, config.getMaxTierDefinido());
                env.setTierCasco(Math.max(0, Math.min(maximo, env.getTierCasco() + (int) signo)));
            }
            case SpaceMenuGUI.SLOT_TRAJE_COMPLETO -> env.setRequiereTrajeCompleto(!env.isRequiereTrajeCompleto());
            case SpaceMenuGUI.SLOT_DANIO_ASFIXIA ->
                    env.setDanioSinCasco(limitar(env.getDanioSinCasco() + 0.5 * signo * paso, 0, 20));

            case SpaceMenuGUI.SLOT_TEMP_ACTIVA -> env.setTemperaturaActiva(!env.isTemperaturaActiva());
            case SpaceMenuGUI.SLOT_TEMP_BASE ->
                    env.setTemperaturaBase(limitar(env.getTemperaturaBase() + 5 * signo * paso, -300, 300));
            case SpaceMenuGUI.SLOT_UMBRAL_FRIO ->
                    env.setUmbralFrio(limitar(env.getUmbralFrio() + 5 * signo * paso, -300, 100));
            case SpaceMenuGUI.SLOT_UMBRAL_CALOR ->
                    env.setUmbralCalor(limitar(env.getUmbralCalor() + 5 * signo * paso, -100, 400));
            case SpaceMenuGUI.SLOT_DESGASTE -> env.setDesgasteArmadura(!env.isDesgasteArmadura());

            case SpaceMenuGUI.SLOT_GRAVEDAD ->
                    env.setGravedad(limitar(env.getGravedad() + 0.05 * signo * paso, 0.0, 5.0));
            case SpaceMenuGUI.SLOT_PRESION ->
                    env.setPresion(env.getPresion() + 0.1 * signo * paso);
            case SpaceMenuGUI.SLOT_NOCHE -> {
                env.setBloquearHora(!env.isBloquearHora());
                if (env.isBloquearHora()) {
                    env.setHoraFija(18000);
                }
            }
            case SpaceMenuGUI.SLOT_FUEGO -> s.setProhibirFuegoSinOxigeno(!s.isProhibirFuegoSinOxigeno());
            case SpaceMenuGUI.SLOT_MOBS_CASCO -> s.setMobsConCasco(!s.isMobsConCasco());
            case SpaceMenuGUI.SLOT_MOBS_NORMALES -> s.setBloquearMobsNormales(!s.isBloquearMobsNormales());
            case SpaceMenuGUI.SLOT_ESTRELLAS -> s.setEstrellas(!s.isEstrellas());
            case SpaceMenuGUI.SLOT_TORMENTAS -> s.setTormentasSolares(!s.isTormentasSolares());

            case SpaceMenuGUI.SLOT_BASES -> {
                clic(jugador);
                plugin.getBasesGUI().abrir(jugador);
                return;
            }
            case SpaceMenuGUI.SLOT_TRAJES -> {
                clic(jugador);
                plugin.getSuitGUI().abrirLista(jugador);
                return;
            }
            case SpaceMenuGUI.SLOT_HACHA -> {
                jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
                jugador.sendMessage("§b✔ Hacha de seleccion entregada. §7Clic izquierdo = pos 1, clic derecho = pos 2.");
                clic(jugador);
                return;
            }
            case SpaceMenuGUI.SLOT_MAPA -> {
                clic(jugador);
                plugin.getStarMapGUI().abrir(jugador);
                return;
            }
            default -> {
                return;
            }
        }

        clic(jugador);
        config.guardarTodo();
        plugin.getSpaceMenuGUI().abrir(jugador);
    }

    // ============================================================
    //  BASES ESPACIALES
    // ============================================================

    private void menuBases(InventoryClickEvent event, Player jugador) {
        int slot = event.getRawSlot();
        ClickType tipo = event.getClick();
        List<SpaceBase> lista = plugin.getBasesGUI().listar();

        if (slot == BasesGUI.SLOT_VOLVER) {
            clic(jugador);
            plugin.getSpaceMenuGUI().abrir(jugador);
            return;
        }

        if (slot == BasesGUI.SLOT_CREAR) {
            crearBase(jugador, tipo.isShiftClick());
            return;
        }

        if (slot < 0 || slot >= lista.size()) {
            return;
        }
        SpaceBase base = lista.get(slot);

        if (tipo == ClickType.CONTROL_DROP) {
            plugin.getBaseManager().eliminar(base.getId());
            jugador.sendMessage("§c✖ Base '" + base.getNombre() + "' eliminada.");
        } else if (tipo == ClickType.DROP) {
            try {
                Seleccion sel = seleccionCualquiera(jugador);
                base.setMundo(sel.mundo().getName());
                base.setArea(sel.minX(), sel.minY(), sel.minZ(), sel.maxX(), sel.maxY(), sel.maxZ());
                plugin.getBaseManager().guardar();
                jugador.sendMessage("§a✔ Zona de '" + base.getNombre() + "' redefinida (" + sel.resumen() + ").");
            } catch (IllegalStateException e) {
                jugador.sendMessage("§c" + e.getMessage());
                return;
            }
        } else if (tipo == ClickType.SHIFT_LEFT) {
            base.setTemperatura(base.getTemperatura() + 1);
            plugin.getBaseManager().guardar();
        } else if (tipo == ClickType.SHIFT_RIGHT) {
            base.setTemperatura(base.getTemperatura() - 1);
            plugin.getBaseManager().guardar();
        } else if (tipo == ClickType.RIGHT) {
            base.setOxigeno(!base.isOxigeno());
            plugin.getBaseManager().guardar();
        } else if (tipo == ClickType.LEFT) {
            World mundo = Bukkit.getWorld(base.getMundo());
            if (mundo == null) {
                jugador.sendMessage("§cEl mundo '" + base.getMundo() + "' no esta cargado.");
                return;
            }
            jugador.closeInventory();
            jugador.teleport(new Location(mundo, base.getCentroX(), base.getMinY() + 1.0, base.getCentroZ()));
            jugador.sendMessage("§aTeletransportado a la base '" + base.getNombre() + "'.");
            return;
        } else {
            return;
        }

        clic(jugador);
        plugin.getBasesGUI().abrir(jugador);
    }

    /** Pide el nombre por chat y crea una base con la seleccion (hacha, o WorldEdit si 'worldEdit'). */
    private void crearBase(Player jugador, boolean worldEdit) {
        final Seleccion sel;
        try {
            sel = plugin.getSelectionManager().obtener(jugador, worldEdit);
        } catch (IllegalStateException e) {
            jugador.sendMessage("§c" + e.getMessage());
            return;
        }

        plugin.getPromptManager().pedir(jugador, "Escribe el nombre de la nueva base espacial.", texto -> {
            String id = Textos.aId(texto);
            String candidato = id;
            int n = 2;
            while (plugin.getBaseManager().get(candidato) != null) {
                candidato = id + "-" + n++;
            }
            SpaceBase base = new SpaceBase(candidato, Textos.color(texto), sel.mundo().getName(),
                    sel.minX(), sel.minY(), sel.minZ(), sel.maxX(), sel.maxY(), sel.maxZ());
            plugin.getBaseManager().registrar(base);
            jugador.sendMessage("§a✔ Base '" + base.getNombre() + "§a' creada (" + sel.resumen()
                    + "). Dentro hay oxigeno y " + String.format("%.0f°C", base.getTemperatura()) + ".");
            plugin.getBasesGUI().abrir(jugador);
        });
    }

    /** Intenta la seleccion del hacha y, si no hay, la de WorldEdit. */
    private Seleccion seleccionCualquiera(Player jugador) {
        try {
            return plugin.getSelectionManager().obtener(jugador, false);
        } catch (IllegalStateException sinHacha) {
            try {
                return plugin.getSelectionManager().obtener(jugador, true);
            } catch (IllegalStateException sinWorldEdit) {
                throw sinHacha; // el mensaje del hacha es el mas util
            }
        }
    }

    // ============================================================
    //  TRAJES: lista de tiers
    // ============================================================

    private void menuListaTrajes(InventoryClickEvent event, Player jugador) {
        int slot = event.getRawSlot();

        if (slot == SuitGUI.SLOT_VOLVER_LISTA) {
            clic(jugador);
            plugin.getSpaceMenuGUI().abrir(jugador);
            return;
        }

        if (slot == SuitGUI.SLOT_ANADIR) {
            if (config.anadirTraje() == null) {
                jugador.sendMessage("§cYa hay el maximo de tiers (" + SuitTier.MAX_TIERS + ").");
                return;
            }
            config.guardarTodo();
            clic(jugador);
            plugin.getSuitGUI().abrirLista(jugador);
            return;
        }

        if (slot == SuitGUI.SLOT_QUITAR) {
            if (!event.getClick().isShiftClick()) {
                jugador.sendMessage("§cUsa Shift + clic para confirmar que quieres quitar el ultimo tier.");
                return;
            }
            if (config.quitarUltimoTraje()) {
                config.guardarTodo();
                clic(jugador);
                plugin.getSuitGUI().abrirLista(jugador);
            }
            return;
        }

        List<SuitTier> tiers = new ArrayList<>(config.getTrajes().values());
        for (int i = 0; i < SuitGUI.SLOTS_TIER.length && i < tiers.size(); i++) {
            if (SuitGUI.SLOTS_TIER[i] == slot) {
                clic(jugador);
                plugin.getSuitGUI().abrirTier(jugador, tiers.get(i));
                return;
            }
        }
    }

    // ============================================================
    //  TRAJES: editor de un tier
    // ============================================================

    private void menuTier(InventoryClickEvent event, Player jugador, int numeroTier) {
        SuitTier t = config.getTraje(numeroTier);
        if (t == null) {
            plugin.getSuitGUI().abrirLista(jugador);
            return;
        }

        ClickType tipo = event.getClick();
        boolean izq = tipo.isLeftClick();
        double signo = izq ? 1 : -1;
        double paso = tipo.isShiftClick() ? 5.0 : 1.0;
        SuitManager suits = plugin.getSuitManager();

        switch (event.getRawSlot()) {
            case SuitGUI.SLOT_MATERIAL -> {
                List<String> familias = SuitTier.FAMILIAS;
                int i = Math.max(0, familias.indexOf(t.getMaterial()));
                i = izq ? (i + 1) % familias.size() : (i - 1 + familias.size()) % familias.size();
                t.setMaterial(familias.get(i));
            }
            case SuitGUI.SLOT_NOMBRE -> {
                plugin.getPromptManager().pedir(jugador, "Escribe el nombre del traje (admite &colores).", texto -> {
                    t.setNombre(texto);
                    config.guardarTodo();
                    plugin.getSuitGUI().abrirTier(jugador, t);
                });
                return;
            }
            case SuitGUI.SLOT_COLOR -> {
                if (!t.usaCuero()) {
                    jugador.sendMessage("§cEl color solo afecta a los trajes de cuero (material LEATHER).");
                    return;
                }
                int[] colores = SuitGUI.COLORES;
                int i = 0;
                for (int k = 0; k < colores.length; k++) {
                    if (colores[k] == t.getColor()) {
                        i = k;
                    }
                }
                i = izq ? (i + 1) % colores.length : (i - 1 + colores.length) % colores.length;
                t.setColor(colores[i]);
            }
            case SuitGUI.SLOT_MODELO ->
                    t.setCustomModelData(t.getCustomModelData() + (int) (signo * (tipo.isShiftClick() ? 10 : 1)));
            case SuitGUI.SLOT_FRIO -> t.setProteccionFrio(t.getProteccionFrio() + 5 * signo * paso);
            case SuitGUI.SLOT_CALOR -> t.setProteccionCalor(t.getProteccionCalor() + 5 * signo * paso);
            case SuitGUI.SLOT_PRESION -> {
                double v = t.getResistenciaPresion();
                double base = (v < 10) ? 1 : (v < 50 ? 5 : 10);
                t.setResistenciaPresion(v + base * signo * paso);
            }
            case SuitGUI.SLOT_DAR_CASCO -> {
                darItems(jugador, List.of(suits.crearPieza(t, "HELMET")));
                clic(jugador);
                return;
            }
            case SuitGUI.SLOT_DAR_TRAJE -> {
                darItems(jugador, suits.crearTrajeCompleto(t));
                clic(jugador);
                return;
            }
            case SuitGUI.SLOT_VOLVER -> {
                clic(jugador);
                plugin.getSuitGUI().abrirLista(jugador);
                return;
            }
            default -> {
                return;
            }
        }

        clic(jugador);
        config.guardarTodo();
        plugin.getSuitGUI().abrirTier(jugador, t);
    }

    private void darItems(Player jugador, List<ItemStack> items) {
        for (ItemStack item : items) {
            for (ItemStack resto : jugador.getInventory().addItem(item).values()) {
                jugador.getWorld().dropItemNaturally(jugador.getLocation(), resto);
            }
        }
        jugador.sendMessage("§a✔ Recibido: " + items.size() + (items.size() == 1 ? " pieza" : " piezas") + " de traje.");
    }

    // ------------------------------------------------------------

    private void clic(Player jugador) {
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    private double limitar(double valor, double min, double max) {
        double recortado = Math.max(min, Math.min(max, valor));
        return Math.round(recortado * 100.0) / 100.0;
    }
}
