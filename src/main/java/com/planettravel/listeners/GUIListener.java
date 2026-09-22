package com.planettravel.listeners;

import com.planettravel.PlanetTravel;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.gui.EffectsGUI;
import com.planettravel.gui.PlanetEditorGUI;
import com.planettravel.gui.StarMapGUI;
import com.planettravel.selection.AreaPlaneta;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * Maneja los clics de las tres GUIs: mapa estelar, editor de planeta y
 * selector de efectos.
 *
 * Todas las GUIs son de solo lectura en cuanto a mover items: cancelamos
 * siempre el evento para que nadie pueda sacar los iconos del inventario.
 */
public class GUIListener implements Listener {

    /** Cascos ofrecidos al rotar con clic en el editor. */
    private static final List<String> CASCOS = Arrays.asList(
            "TURTLE_HELMET", "IRON_HELMET", "GOLDEN_HELMET", "DIAMOND_HELMET",
            "NETHERITE_HELMET", "CHAINMAIL_HELMET", "LEATHER_HELMET", "GLASS"
    );

    private final PlanetTravel plugin;
    private final PlanetConfigManager configManager;
    private final StarMapGUI starMapGUI;
    private final PlanetEditorGUI editorGUI;
    private final EffectsGUI effectsGUI;

    public GUIListener(PlanetTravel plugin, PlanetConfigManager configManager, StarMapGUI starMapGUI,
                       PlanetEditorGUI editorGUI, EffectsGUI effectsGUI) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.starMapGUI = starMapGUI;
        this.editorGUI = editorGUI;
        this.effectsGUI = effectsGUI;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        String titulo = event.getView().getTitle();

        if (titulo.equals(StarMapGUI.TITULO)) {
            event.setCancelled(true);
            manejarMapaEstelar(event);
            return;
        }

        if (titulo.startsWith(PlanetEditorGUI.PREFIJO_TITULO)) {
            event.setCancelled(true);
            manejarEditor(event, PlanetEditorGUI.idDesdeTitulo(titulo));
            return;
        }

        if (titulo.startsWith(EffectsGUI.PREFIJO_TITULO)) {
            event.setCancelled(true);
            manejarEfectos(event, EffectsGUI.idDesdeTitulo(titulo));
        }
    }

    // ------------------------------------------------------------
    //  MAPA ESTELAR
    // ------------------------------------------------------------

    private void manejarMapaEstelar(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player jugador)) return;
        if (!jugador.hasPermission("planettravel.admin")) return;

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) return;

        // Los planetas se colocan en orden en los primeros slots.
        List<PlanetData> lista = configManager.getPlanetas().values().stream().toList();
        if (slot >= lista.size()) return;

        clic(jugador);
        editorGUI.abrir(jugador, lista.get(slot));
    }

    // ------------------------------------------------------------
    //  EDITOR DE PLANETA
    // ------------------------------------------------------------

    private void manejarEditor(InventoryClickEvent event, String idPlaneta) {
        if (!(event.getWhoClicked() instanceof Player jugador)) return;
        if (idPlaneta == null) return;
        if (!jugador.hasPermission("planettravel.admin")) return;

        PlanetData planeta = configManager.getPlaneta(idPlaneta).orElse(null);
        if (planeta == null) return;

        PlanetEnvironment env = planeta.getEntorno();
        int slot = event.getRawSlot();
        ClickType tipo = event.getClick();

        // Clic izquierdo = subir/activar; derecho = bajar/desactivar.
        boolean izquierdo = tipo.isLeftClick();
        // Shift multiplica el paso por 5 para ajustes rapidos.
        double paso = tipo.isShiftClick() ? 5.0 : 1.0;

        switch (slot) {
            case PlanetEditorGUI.SLOT_CASCO ->
                    env.setRequiereCasco(!env.isRequiereCasco());

            case PlanetEditorGUI.SLOT_MATERIAL_CASCO -> {
                int indice = CASCOS.indexOf(env.getMaterialCasco());
                indice = izquierdo
                        ? (indice + 1) % CASCOS.size()
                        : (indice - 1 + CASCOS.size()) % CASCOS.size();
                env.setMaterialCasco(CASCOS.get(indice));
            }

            case PlanetEditorGUI.SLOT_TRAJE ->
                    env.setRequiereTrajeCompleto(!env.isRequiereTrajeCompleto());

            case PlanetEditorGUI.SLOT_DANIO_CASCO ->
                    env.setDanioSinCasco(limitar(env.getDanioSinCasco() + (izquierdo ? 0.5 : -0.5) * paso, 0, 20));

            case PlanetEditorGUI.SLOT_AGUA_TOXICA ->
                    env.setAguaToxica(!env.isAguaToxica());

            case PlanetEditorGUI.SLOT_DANIO_AGUA ->
                    env.setDanioAgua(limitar(env.getDanioAgua() + (izquierdo ? 0.5 : -0.5) * paso, 0, 20));

            case PlanetEditorGUI.SLOT_AGUA_VENENO ->
                    env.setAguaEnvenena(!env.isAguaEnvenena());

            case PlanetEditorGUI.SLOT_GRAVEDAD ->
                    env.setGravedad(env.getGravedad() + (izquierdo ? 0.05 : -0.05) * paso);

            case PlanetEditorGUI.SLOT_TEMP_ACTIVA ->
                    env.setTemperaturaActiva(!env.isTemperaturaActiva());

            case PlanetEditorGUI.SLOT_TEMP_BASE ->
                    env.setTemperaturaBase(limitar(env.getTemperaturaBase() + (izquierdo ? 1 : -1) * paso, -200, 300));

            case PlanetEditorGUI.SLOT_TEMP_UMBRAL_CALOR ->
                    env.setUmbralCalor(limitar(env.getUmbralCalor() + (izquierdo ? 1 : -1) * paso, -100, 400));

            case PlanetEditorGUI.SLOT_TEMP_UMBRAL_FRIO ->
                    env.setUmbralFrio(limitar(env.getUmbralFrio() + (izquierdo ? 1 : -1) * paso, -300, 100));

            case PlanetEditorGUI.SLOT_TEMP_ALTURA ->
                    env.setVariacionPorAltura(limitar(env.getVariacionPorAltura() + (izquierdo ? 0.5 : -0.5) * paso, -50, 50));

            case PlanetEditorGUI.SLOT_TEMP_DIA_NOCHE ->
                    env.setVariacionDiaNoche(limitar(env.getVariacionDiaNoche() + (izquierdo ? 1 : -1) * paso, 0, 150));

            case PlanetEditorGUI.SLOT_EFECTOS -> {
                clic(jugador);
                effectsGUI.abrir(jugador, planeta);
                return; // no reabrimos el editor
            }

            case PlanetEditorGUI.SLOT_CLIMA -> rotarClima(env);

            case PlanetEditorGUI.SLOT_HORA -> {
                if (izquierdo) {
                    env.setBloquearHora(!env.isBloquearHora());
                } else {
                    // Avanza en bloques de 3000 ticks: amanecer, mediodia, tarde, noche...
                    env.setHoraFija((env.getHoraFija() + 3000) % 24000);
                }
            }

            case PlanetEditorGUI.SLOT_ICONO -> {
                // Usa el item que el jugador lleve en el cursor como icono.
                ItemStack cursor = event.getCursor();
                if (cursor != null && cursor.getType() != Material.AIR) {
                    planeta.setIconoMaterial(cursor.getType().name());
                } else {
                    jugador.sendMessage("§7Coge un item en el cursor y haz clic aqui para usarlo como icono.");
                }
            }

            // ---------------- Nuevo en 1.2.0 ----------------

            case PlanetEditorGUI.SLOT_TIER_CASCO -> {
                int maximo = Math.max(1, configManager.getMaxTierDefinido());
                int nuevo = env.getTierCasco() + (izquierdo ? 1 : -1);
                env.setTierCasco(Math.max(0, Math.min(maximo, nuevo)));
            }

            case PlanetEditorGUI.SLOT_PRESION -> {
                double actual = env.getPresion();
                double base = (actual < 2) ? 0.1 : (actual < 10 ? 0.5 : 2.5);
                env.setPresion(actual + (izquierdo ? base : -base) * paso);
            }

            case PlanetEditorGUI.SLOT_DESGASTE ->
                    env.setDesgasteArmadura(!env.isDesgasteArmadura());

            case PlanetEditorGUI.SLOT_MOBS_CASCO ->
                    env.setMobsConCasco(!env.isMobsConCasco());

            case PlanetEditorGUI.SLOT_METEORITOS ->
                    env.setMeteoritos(!env.isMeteoritos());

            case PlanetEditorGUI.SLOT_PARTICULAS -> {
                List<String> lista = PlanetEnvironment.PARTICULAS;
                int indice = Math.max(0, lista.indexOf(env.getParticulasAmbiente()));
                indice = izquierdo
                        ? (indice + 1) % lista.size()
                        : (indice - 1 + lista.size()) % lista.size();
                env.setParticulasAmbiente(lista.get(indice));
            }

            case PlanetEditorGUI.SLOT_DESCRIPCION -> {
                if (tipo.isShiftClick() && !izquierdo) {
                    planeta.limpiarDescripcion();
                    break;
                }
                boolean anadir = !izquierdo;
                plugin.getPromptManager().pedir(jugador,
                        anadir ? "Escribe la linea que quieres ANADIR a la descripcion (admite &colores)."
                               : "Escribe la nueva descripcion (usa | para separar lineas, admite &colores).",
                        texto -> {
                            if (anadir) {
                                planeta.anadirLineaDescripcion(texto);
                            } else {
                                planeta.setDescripcion(texto);
                            }
                            configManager.guardarTodo();
                            editorGUI.abrir(jugador, planeta);
                        });
                return;
            }

            case PlanetEditorGUI.SLOT_NOMBRE -> {
                plugin.getPromptManager().pedir(jugador, "Escribe el nuevo nombre visible del planeta.", texto -> {
                    planeta.setNombreVisible(texto);
                    configManager.guardarTodo();
                    editorGUI.abrir(jugador, planeta);
                });
                return;
            }

            case PlanetEditorGUI.SLOT_CLASIFICACION -> {
                plugin.getPromptManager().pedir(jugador,
                        "Escribe la clasificacion (ej: Desertico, Gigante gaseoso, Volcanico).", texto -> {
                    planeta.setClasificacion(texto);
                    configManager.guardarTodo();
                    editorGUI.abrir(jugador, planeta);
                });
                return;
            }

            case PlanetEditorGUI.SLOT_ENTRADA -> {
                jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
                plugin.getSelectionManager().activarModoPunto(jugador,
                        com.planettravel.selection.SelectionManager.TipoPunto.ENTRADA, planeta.getId());
                jugador.sendMessage("§b✔ Hacha entregada. §7Ve al mundo §f" + planeta.getWorldName()
                        + "§7: clic §fizquierdo§7=esquina A, clic §fderecho§7=esquina B de la zona de aterrizaje.");
                clic(jugador);
                return;
            }

            case PlanetEditorGUI.SLOT_ENTRADA_MODO -> {
                planeta.setEntradaUsaWorldBorder(!planeta.isEntradaUsaWorldBorder());
                if (planeta.isEntradaUsaWorldBorder()) {
                    jugador.sendMessage("§a✔ Entrada aleatoria dentro del WorldBorder de '" + planeta.getWorldName()
                            + "'. §7Revisa que tengas un §f/worldborder §7razonable puesto en ese mundo.");
                } else {
                    jugador.sendMessage("§7Entrada: de vuelta a la zona marcada con el hacha.");
                }
            }

            case PlanetEditorGUI.SLOT_DETECCION -> {
                if (izquierdo) {
                    jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
                    plugin.getSelectionManager().activarModoPunto(jugador,
                            com.planettravel.selection.SelectionManager.TipoPunto.DETECCION, planeta.getId());
                    jugador.sendMessage("§b✔ Hacha entregada. §7Ve al espacio: clic §fizquierdo§7=esquina A, "
                            + "clic §fderecho§7=esquina B de la caja de deteccion.");
                    clic(jugador);
                    return;
                } else {
                    planeta.setDeteccionUsaCaja(!planeta.isDeteccionUsaCaja());
                }
            }

            case PlanetEditorGUI.SLOT_SALIDA ->
                    planeta.setSalidaMargen(Math.max(5, planeta.getSalidaMargen() + (izquierdo ? 5 : -5) * paso));

            case PlanetEditorGUI.SLOT_AREA -> {
                if (izquierdo && !tipo.isShiftClick()) {
                    jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
                    jugador.sendMessage("§b✔ Hacha de seleccion entregada. §7Marca la esfera del planeta en el espacio "
                            + "(izq = pos 1, der = pos 2) y vuelve aqui: clic derecho para aplicar.");
                    clic(jugador);
                    return;
                }
                try {
                    // Clic derecho = seleccion del hacha; Shift + derecho = seleccion de WorldEdit.
                    jugador.sendMessage(AreaPlaneta.aplicarDesde(plugin, jugador, planeta, tipo.isShiftClick()));
                } catch (IllegalStateException e) {
                    jugador.sendMessage("§c" + e.getMessage());
                    return;
                }
            }

            case PlanetEditorGUI.SLOT_TRAJES -> {
                clic(jugador);
                plugin.getSuitGUI().abrirLista(jugador);
                return;
            }

            case PlanetEditorGUI.SLOT_VOLVER -> {
                clic(jugador);
                starMapGUI.abrir(jugador);
                return;
            }

            default -> {
                return; // clic en relleno o en el inventario del jugador
            }
        }

        clic(jugador);
        configManager.guardarTodo();
        editorGUI.abrir(jugador, planeta); // refrescamos para que se vea el cambio
    }

    /** Rota el clima entre: libre → despejado fijo → lluvia fija → libre. */
    private void rotarClima(PlanetEnvironment env) {
        if (!env.isBloquearClima()) {
            env.setBloquearClima(true);
            env.setLluviaPermanente(false);
        } else if (!env.isLluviaPermanente()) {
            env.setLluviaPermanente(true);
        } else {
            env.setBloquearClima(false);
            env.setLluviaPermanente(false);
        }
    }

    // ------------------------------------------------------------
    //  SELECTOR DE EFECTOS
    // ------------------------------------------------------------

    private void manejarEfectos(InventoryClickEvent event, String idPlaneta) {
        if (!(event.getWhoClicked() instanceof Player jugador)) return;
        if (idPlaneta == null) return;
        if (!jugador.hasPermission("planettravel.admin")) return;

        PlanetData planeta = configManager.getPlaneta(idPlaneta).orElse(null);
        if (planeta == null) return;

        int slot = event.getRawSlot();

        // Boton de volver
        if (slot == 31) {
            clic(jugador);
            editorGUI.abrir(jugador, planeta);
            return;
        }

        String efecto = EffectsGUI.efectoEnSlot(slot);
        if (efecto == null) {
            return;
        }

        var efectos = planeta.getEntorno().getEfectosPermanentes();
        Integer nivelActual = efectos.get(efecto);

        if (event.getClick().isLeftClick()) {
            // Activar, o subir nivel hasta un maximo razonable de 5.
            int nuevo = (nivelActual == null) ? 1 : Math.min(5, nivelActual + 1);
            efectos.put(efecto, nuevo);
        } else {
            // Bajar nivel, y quitar del todo al llegar a 0.
            if (nivelActual != null) {
                int nuevo = nivelActual - 1;
                if (nuevo <= 0) {
                    efectos.remove(efecto);
                } else {
                    efectos.put(efecto, nuevo);
                }
            }
        }

        clic(jugador);
        configManager.guardarTodo();
        effectsGUI.abrir(jugador, planeta);
    }

    // ------------------------------------------------------------

    private void clic(Player jugador) {
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.4f);
    }

    /** Recorta un valor entre un minimo y un maximo, y lo redondea a 2 decimales. */
    private double limitar(double valor, double min, double max) {
        double recortado = Math.max(min, Math.min(max, valor));
        return Math.round(recortado * 100.0) / 100.0;
    }
}
