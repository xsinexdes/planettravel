package com.planettravel.gui;

import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EDITOR DE PLANETA: GUI donde el admin configura todo el entorno de un planeta
 * sin tocar el YAML — si el agua es toxica, si hace falta casco espacial, la
 * gravedad, la temperatura, los efectos permanentes, el clima...
 *
 * Convencion de clics en todo el editor:
 *   - CLIC IZQUIERDO  = activar / subir el valor
 *   - CLIC DERECHO    = desactivar / bajar el valor
 *   - SHIFT           = paso grande (x5) en los valores numericos
 *
 * El titulo del inventario lleva el id del planeta embebido para que el
 * listener sepa que planeta esta editando sin tener que guardar estado.
 */
public class PlanetEditorGUI {

    public static final String PREFIJO_TITULO = "§0Editar: ";

    /** Slots de cada ajuste dentro del inventario de 54 ranuras. */
    public static final int SLOT_CASCO = 10;
    public static final int SLOT_MATERIAL_CASCO = 11;
    public static final int SLOT_TRAJE = 12;
    public static final int SLOT_DANIO_CASCO = 13;

    public static final int SLOT_AGUA_TOXICA = 19;
    public static final int SLOT_DANIO_AGUA = 20;
    public static final int SLOT_AGUA_VENENO = 21;

    public static final int SLOT_GRAVEDAD = 28;

    public static final int SLOT_TEMP_ACTIVA = 15;
    public static final int SLOT_TEMP_BASE = 16;
    public static final int SLOT_TEMP_UMBRAL_CALOR = 24;
    public static final int SLOT_TEMP_UMBRAL_FRIO = 25;
    public static final int SLOT_TEMP_ALTURA = 33;
    public static final int SLOT_TEMP_DIA_NOCHE = 34;

    public static final int SLOT_EFECTOS = 30;
    public static final int SLOT_CLIMA = 31;
    public static final int SLOT_HORA = 32;

    // --- Nuevos en 1.2.0 ---
    public static final int SLOT_TIER_CASCO = 14;
    public static final int SLOT_PRESION = 22;
    public static final int SLOT_DESGASTE = 23;
    public static final int SLOT_MOBS_CASCO = 29;
    public static final int SLOT_METEORITOS = 35;
    public static final int SLOT_PARTICULAS = 37;
    public static final int SLOT_DESCRIPCION = 38;
    public static final int SLOT_NOMBRE = 39;
    public static final int SLOT_CLASIFICACION = 40;
    public static final int SLOT_AREA = 46;
    public static final int SLOT_TRAJES = 47;

    public static final int SLOT_ICONO = 49;
    public static final int SLOT_VOLVER = 45;
    public static final int SLOT_INFO = 4;

    /** Construye el titulo del inventario para un planeta dado. */
    public static String tituloPara(PlanetData planeta) {
        return PREFIJO_TITULO + planeta.getId();
    }

    /** Extrae el id del planeta desde el titulo del inventario. */
    public static String idDesdeTitulo(String titulo) {
        if (!titulo.startsWith(PREFIJO_TITULO)) {
            return null;
        }
        return titulo.substring(PREFIJO_TITULO.length());
    }

    public void abrir(Player jugador, PlanetData planeta) {
        Inventory inv = Bukkit.createInventory(null, 54, tituloPara(planeta));
        PlanetEnvironment env = planeta.getEntorno();

        rellenarFondo(inv);

        // ---- Cabecera informativa ----
        inv.setItem(SLOT_INFO, item(planeta.getIcono(),
                "§b§l" + planeta.getNombreVisible(),
                "§7Mundo: §f" + planeta.getWorldName(),
                "§7Entrada atmosferica: §fY " + (int) planeta.getEntradaAtmosfericaY(),
                "§7Altura de escape: §fY " + (int) planeta.getAlturaDespegueY(),
                "§7Radio de deteccion: §f" + (int) planeta.getRadio(),
                "",
                "§8Las coordenadas se editan con /planeta editar"));

        // ---- ATMOSFERA ----
        inv.setItem(SLOT_CASCO, toggle(Material.TURTLE_HELMET, "§6Casco espacial obligatorio",
                env.isRequiereCasco(),
                "§7Si esta activo, el jugador sufre",
                "§7asfixia sin el casco correcto."));

        inv.setItem(SLOT_MATERIAL_CASCO, item(materialSeguro(env.getMaterialCasco()),
                "§6Tipo de casco requerido",
                "§7Actual: §f" + StarMapGUI.formatearMaterial(env.getMaterialCasco()),
                "",
                "§eClic §7para rotar entre opciones comunes",
                "§8(o usa /planeta editar para uno exacto)"));

        inv.setItem(SLOT_TRAJE, toggle(Material.LEATHER_CHESTPLATE, "§6Traje completo obligatorio",
                env.isRequiereTrajeCompleto(),
                "§7Ademas del casco, exige las",
                "§74 piezas de armadura puestas."));

        inv.setItem(SLOT_DANIO_CASCO, valor(Material.WITHER_SKELETON_SKULL,
                "§6Danio por asfixia",
                String.format("%.1f", env.getDanioSinCasco()) + " §7por segundo"));

        // Tier de traje: 0 = modo clasico (usa el material de arriba)
        ItemStack itemTier = item(Material.NETHERITE_HELMET, "§6Tier de traje requerido",
                env.getTierCasco() > 0
                        ? "§7Actual: §fTier " + env.getTierCasco()
                        : "§7Actual: §f0 §8(modo clasico: usa el material de casco)",
                "",
                "§eClic izq §7subir tier  §c| §eClic der §7bajar",
                "§8Con tier > 0 se ignora el material de casco.",
                "§8Los tiers se editan en el menu de trajes.");
        itemTier.setAmount(Math.max(1, Math.min(64, env.getTierCasco())));
        inv.setItem(SLOT_TIER_CASCO, itemTier);

        // ---- AGUA ----
        inv.setItem(SLOT_AGUA_TOXICA, toggle(Material.WATER_BUCKET, "§2Agua toxica",
                env.isAguaToxica(),
                "§7El agua de este planeta hace",
                "§7danio al contacto."));

        inv.setItem(SLOT_DANIO_AGUA, valor(Material.SPIDER_EYE,
                "§2Danio del agua",
                String.format("%.1f", env.getDanioAgua()) + " §7por segundo"));

        inv.setItem(SLOT_AGUA_VENENO, toggle(Material.FERMENTED_SPIDER_EYE, "§2El agua envenena",
                env.isAguaEnvenena(),
                "§7Aplica veneno que persiste",
                "§7unos segundos tras salir."));

        // ---- PRESION / ARMADURA ----
        inv.setItem(SLOT_PRESION, valor(Material.GLASS_BOTTLE, "§bPresion atmosferica",
                String.format("%.2f atm", env.getPresion()) + " §7(" + env.getNombreAtmosfera() + ")"));
        inv.setItem(SLOT_DESGASTE, toggle(Material.IRON_CHESTPLATE, "§bFrio/calor desgastan la armadura",
                env.isDesgasteArmadura(),
                "§7Con temperatura extrema la armadura",
                "§7se congela o se sobrecalienta."));

        // ---- GRAVEDAD ----
        inv.setItem(SLOT_GRAVEDAD, valor(Material.ANVIL,
                "§dGravedad",
                String.format("%.2f", env.getGravedad()) + "x " + descripcionGravedad(env.getGravedad())));

        // ---- TEMPERATURA ----
        inv.setItem(SLOT_TEMP_ACTIVA, toggle(Material.BLAZE_POWDER, "§eSistema de temperatura",
                env.isTemperaturaActiva(),
                "§7Activa la simulacion termica",
                "§7completa en este planeta."));

        inv.setItem(SLOT_TEMP_BASE, valor(Material.CAMPFIRE,
                "§eTemperatura base",
                String.format("%.0f°C", env.getTemperaturaBase())));

        inv.setItem(SLOT_TEMP_UMBRAL_CALOR, valor(Material.MAGMA_BLOCK,
                "§cUmbral de calor",
                String.format("%.0f°C", env.getUmbralCalor()) + " §7— por encima, danio"));

        inv.setItem(SLOT_TEMP_UMBRAL_FRIO, valor(Material.BLUE_ICE,
                "§bUmbral de frio",
                String.format("%.0f°C", env.getUmbralFrio()) + " §7— por debajo, danio"));

        inv.setItem(SLOT_TEMP_ALTURA, valor(Material.FEATHER,
                "§eVariacion por altura",
                String.format("%.1f°C", env.getVariacionPorAltura()) + " §7por cada 100 bloques"));

        inv.setItem(SLOT_TEMP_DIA_NOCHE, valor(Material.CLOCK,
                "§eVariacion dia/noche",
                "±" + String.format("%.0f°C", env.getVariacionDiaNoche())));

        // ---- EFECTOS Y AMBIENTE ----
        List<String> loreEfectos = new ArrayList<>();
        loreEfectos.add("§7Efectos aplicados constantemente");
        loreEfectos.add("§7a quien este en el planeta.");
        loreEfectos.add("");
        if (env.getEfectosPermanentes().isEmpty()) {
            loreEfectos.add("§8Ninguno configurado");
        } else {
            for (var e : env.getEfectosPermanentes().entrySet()) {
                loreEfectos.add("§5 • §d" + StarMapGUI.formatearMaterial(e.getKey()) + " §7nivel " + e.getValue());
            }
        }
        loreEfectos.add("");
        loreEfectos.add("§eClic §7para abrir el selector de efectos");
        inv.setItem(SLOT_EFECTOS, item(Material.BREWING_STAND, "§5Efectos permanentes",
                loreEfectos.toArray(new String[0])));

        inv.setItem(SLOT_CLIMA, toggle(Material.SNOWBALL,
                env.isBloquearClima()
                        ? (env.isLluviaPermanente() ? "§9Clima: lluvia fija" : "§9Clima: despejado fijo")
                        : "§9Clima: libre",
                env.isBloquearClima(),
                "§eClic izq §7rota: libre → despejado → lluvia"));

        inv.setItem(SLOT_HORA, toggle(Material.CLOCK,
                env.isBloquearHora() ? "§9Hora fija: §f" + env.getHoraFija() : "§9Ciclo dia/noche normal",
                env.isBloquearHora(),
                "§eClic izq §7activa/desactiva hora fija",
                "§eClic der §7avanza la hora fijada"));

        // ---- MOBS Y AMBIENTE ----
        inv.setItem(SLOT_MOBS_CASCO, toggle(Material.GLASS, "§7Mobs con casco de vidrio",
                env.isMobsConCasco(),
                "§7Los mobs humanoides que aparecen",
                "§7aqui llevan casco espacial."));
        inv.setItem(SLOT_METEORITOS, toggle(Material.FIRE_CHARGE, "§6Lluvia de meteoritos",
                env.isMeteoritos(),
                "§7Meteoritos cada ~" + env.getMeteoritosIntervaloSeg() + "s por jugador.",
                "§7Hacen danio al impactar, sin romper bloques."));
        inv.setItem(SLOT_PARTICULAS, item(Material.WHITE_DYE, "§fParticulas ambientales",
                "§7Actual: §f" + env.getParticulasAmbiente(),
                "",
                "§eClic §7para rotar: NONE, POLVO, CENIZA,",
                "§7NIEVE, ESPORAS, BRASAS"));

        // ---- TEXTOS ----
        List<String> loreDesc = new ArrayList<>();
        loreDesc.add("§7Lineas actuales: §f" + planeta.getDescripcionLineas().size());
        for (String linea : planeta.getDescripcionLineas()) {
            loreDesc.add("§8 - §7" + com.planettravel.util.Textos.color(linea));
        }
        loreDesc.add("");
        loreDesc.add("§eClic izq §7reemplazar (usa | para separar lineas)");
        loreDesc.add("§eClic der §7anadir una linea");
        loreDesc.add("§eShift + der §7borrar todo");
        loreDesc.add("§8Admite colores con &");
        inv.setItem(SLOT_DESCRIPCION, item(Material.WRITABLE_BOOK, "§bDescripcion", loreDesc.toArray(new String[0])));

        inv.setItem(SLOT_NOMBRE, item(Material.NAME_TAG, "§bNombre visible",
                "§7Actual: §f" + com.planettravel.util.Textos.color(planeta.getNombreVisible()),
                "", "§eClic §7para cambiarlo por chat"));
        inv.setItem(SLOT_CLASIFICACION, item(Material.PAPER, "§bClasificacion",
                "§7Actual: §f" + (planeta.getClasificacion().isEmpty() ? "§8(ninguna)" : com.planettravel.util.Textos.color(planeta.getClasificacion())),
                "§8Ej: Desertico, Gigante gaseoso, Volcanico",
                "", "§eClic §7para cambiarla por chat"));

        // ---- AREA / TRAJES ----
        inv.setItem(SLOT_AREA, item(Material.WOODEN_AXE, "§bArea del planeta (hacha)",
                "§7Centro: §f" + String.format("%.0f, %.0f, %.0f", planeta.getSpaceX(), planeta.getSpaceY(), planeta.getSpaceZ()),
                "§7Radio visual: §f" + String.format("%.0f", planeta.getRadioVisual())
                        + " §7| deteccion: §f" + String.format("%.0f", planeta.getRadio()),
                "",
                "§eClic izq §7recibir el hacha de seleccion",
                "§eClic der §7usar mi seleccion como area",
                "§eShift + der §7usar la seleccion de WorldEdit"));
        inv.setItem(SLOT_TRAJES, item(Material.LEATHER_HELMET, "§6Trajes espaciales",
                "§7Edita los cascos y trajes de cada tier.", "", "§eClic §7para abrir"));

        // ---- Navegacion ----
        inv.setItem(SLOT_ICONO, item(planeta.getIcono(), "§bIcono del planeta",
                "§7Actual: §f" + StarMapGUI.formatearMaterial(planeta.getIconoMaterial()),
                "",
                "§eClic §7con un item en el cursor",
                "§7para usarlo como icono"));

        inv.setItem(SLOT_VOLVER, item(Material.ARROW, "§7« Volver al mapa estelar"));

        jugador.openInventory(inv);
    }

    // ------------------------------------------------------------
    //  Helpers de construccion de items
    // ------------------------------------------------------------

    private void rellenarFondo(Inventory inv) {
        ItemStack relleno = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, relleno);
        }
    }

    public static ItemStack item(Material material, String nombre, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(nombre);
            if (lore.length > 0) {
                meta.setLore(Arrays.asList(lore));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    /** Item de interruptor: verde si esta activado, gris si no. */
    private ItemStack toggle(Material material, String nombre, boolean activo, String... descripcion) {
        List<String> lore = new ArrayList<>(Arrays.asList(descripcion));
        lore.add("");
        lore.add(activo ? "§a✔ ACTIVADO" : "§c✖ DESACTIVADO");
        lore.add("§eClic §7para cambiar");
        return item(activo ? material : Material.GRAY_DYE, nombre, lore.toArray(new String[0]));
    }

    /** Item de valor numerico ajustable con clic izquierdo/derecho. */
    private ItemStack valor(Material material, String nombre, String valorActual) {
        return item(material, nombre,
                "§7Valor: §f" + valorActual,
                "",
                "§eClic izq §7subir  §c| §eClic der §7bajar",
                "§8Shift = paso grande (x5)");
    }

    private Material materialSeguro(String nombre) {
        Material m = Material.matchMaterial(nombre);
        return (m == null) ? Material.TURTLE_HELMET : m;
    }

    private String descripcionGravedad(double g) {
        if (g < 0.4) return "§7(lunar)";
        if (g < 0.9) return "§7(ligera)";
        if (g < 1.1) return "§7(terrestre)";
        if (g < 1.8) return "§7(pesada)";
        return "§7(aplastante)";
    }
}
