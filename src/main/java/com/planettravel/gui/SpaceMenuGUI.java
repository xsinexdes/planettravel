package com.planettravel.gui;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.config.SpaceSettings;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * MENU DEL ESPACIO (/espacio): configura el propio espacio, no un planeta.
 * Reglas de casco/traje, temperatura, gravedad, presion, fuego, mobs, ambiente,
 * tormentas solares, y acceso a las bases espaciales y a los trajes.
 *
 * Clic izquierdo = activar/subir; clic derecho = desactivar/bajar; Shift = paso x5.
 */
public class SpaceMenuGUI {

    public static final String TITULO = "§0§lMenu del Espacio";

    public static final int SLOT_ACTIVO = 10;
    public static final int SLOT_CASCO = 11;
    public static final int SLOT_TIER = 12;
    public static final int SLOT_TRAJE_COMPLETO = 13;
    public static final int SLOT_DANIO_ASFIXIA = 14;

    public static final int SLOT_TEMP_ACTIVA = 19;
    public static final int SLOT_TEMP_BASE = 20;
    public static final int SLOT_UMBRAL_FRIO = 21;
    public static final int SLOT_UMBRAL_CALOR = 22;
    public static final int SLOT_DESGASTE = 23;

    public static final int SLOT_GRAVEDAD = 28;
    public static final int SLOT_PRESION = 29;
    public static final int SLOT_NOCHE = 30;
    public static final int SLOT_FUEGO = 31;
    public static final int SLOT_MOBS_CASCO = 32;
    public static final int SLOT_MOBS_NORMALES = 33;
    public static final int SLOT_ESTRELLAS = 34;

    public static final int SLOT_TORMENTAS = 37;

    public static final int SLOT_MOBS_TRANSFORMAR = 38;
    public static final int SLOT_MOB_ESPACIAL = 39;
    public static final int SLOT_METEORITOS_ESPACIO = 40;
    public static final int SLOT_METEORITOS_ESPACIO_DANIO = 41;
    public static final int SLOT_SOL_ACTIVO = 42;

    public static final int SLOT_BASES = 43;
    public static final int SLOT_TRAJES = 44;
    public static final int SLOT_SOL_MARCAR = 45;
    public static final int SLOT_HACHA = 46;
    public static final int SLOT_SOL_TEMP_CERCANA = 47;
    public static final int SLOT_SOL_DISTANCIA = 48;
    public static final int SLOT_MAPA = 49;

    private final PlanetConfigManager config;

    public SpaceMenuGUI(PlanetConfigManager config) {
        this.config = config;
    }

    public void abrir(Player jugador) {
        Inventory inv = Bukkit.createInventory(null, 54, TITULO);
        SpaceSettings s = config.getSpaceSettings();
        PlanetEnvironment env = s.getEntorno();

        ItemStack relleno = PlanetEditorGUI.item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 54; i++) {
            inv.setItem(i, relleno);
        }

        inv.setItem(4, PlanetEditorGUI.item(Material.NETHER_STAR, "§b§lEl Espacio",
                "§7Mundo: §f" + config.getMundoEspacio(),
                "§7Estado: " + (s.isActivo() ? "§a✔ hostil (reglas activas)" : "§c✖ desactivado (espacio normal)"),
                "",
                "§8Dentro de una base con oxigeno se anulan",
                "§8las reglas de aire y temperatura."));

        // ---- AIRE ----
        inv.setItem(SLOT_ACTIVO, toggle(Material.ENDER_EYE, "§b§lEspacio hostil (interruptor general)", s.isActivo(),
                "§7Mientras este apagado, el espacio se",
                "§7comporta como un mundo normal."));
        inv.setItem(SLOT_CASCO, toggle(Material.GLASS, "§6Casco obligatorio (sin aire)", env.isRequiereCasco(),
                "§7Sin oxigeno fuera de las bases."));
        inv.setItem(SLOT_TIER, valor(Material.NETHERITE_HELMET, "§6Tier de traje requerido",
                "Tier " + env.getTierCasco() + " §8(0 = casco clasico)"));
        inv.setItem(SLOT_TRAJE_COMPLETO, toggle(Material.IRON_CHESTPLATE, "§6Traje completo obligatorio",
                env.isRequiereTrajeCompleto(), "§7Las 4 piezas de traje del tier."));
        inv.setItem(SLOT_DANIO_ASFIXIA, valor(Material.WITHER_SKELETON_SKULL, "§6Danio por asfixia",
                String.format("%.1f", env.getDanioSinCasco()) + " §7por segundo"));

        // ---- TEMPERATURA ----
        inv.setItem(SLOT_TEMP_ACTIVA, toggle(Material.BLAZE_POWDER, "§eSistema de temperatura", env.isTemperaturaActiva(),
                "§7Frio extremo fuera de las bases."));
        inv.setItem(SLOT_TEMP_BASE, valor(Material.CAMPFIRE, "§eTemperatura del espacio",
                String.format("%.0f°C", env.getTemperaturaBase()) + " §7(paso 5)"));
        inv.setItem(SLOT_UMBRAL_FRIO, valor(Material.BLUE_ICE, "§bUmbral de frio",
                String.format("%.0f°C", env.getUmbralFrio()) + " §7- por debajo, danio"));
        inv.setItem(SLOT_UMBRAL_CALOR, valor(Material.MAGMA_BLOCK, "§cUmbral de calor",
                String.format("%.0f°C", env.getUmbralCalor()) + " §7- por encima, danio"));
        inv.setItem(SLOT_DESGASTE, toggle(Material.IRON_BOOTS, "§bFrio/calor desgastan la armadura",
                env.isDesgasteArmadura(), "§7La armadura se congela o se", "§7sobrecalienta con temperaturas extremas."));

        // ---- FISICA Y REGLAS ----
        inv.setItem(SLOT_GRAVEDAD, valor(Material.ANVIL, "§dGravedad del espacio",
                String.format("%.2f", env.getGravedad()) + "x " + PlanetInfo.etiquetaGravedad(env.getGravedad())));
        inv.setItem(SLOT_PRESION, valor(Material.GLASS_BOTTLE, "§bPresion",
                String.format("%.2f atm", env.getPresion()) + " §7(" + env.getNombreAtmosfera() + ")"));
        inv.setItem(SLOT_NOCHE, toggle(Material.CLOCK, "§9Siempre de noche", env.isBloquearHora(),
                "§7Fija la hora a medianoche."));
        inv.setItem(SLOT_FUEGO, toggle(Material.TORCH, "§6Sin fuego donde no hay oxigeno", s.isProhibirFuegoSinOxigeno(),
                "§7Prohibe antorchas, fogatas, velas y",
                "§7encender fuego fuera de las bases."));
        inv.setItem(SLOT_MOBS_CASCO, toggle(Material.ZOMBIE_HEAD, "§7Mobs con casco de vidrio", s.isMobsConCasco(),
                "§7Los mobs humanoides aparecen con",
                "§7casco espacial."));
        inv.setItem(SLOT_MOBS_NORMALES, toggle(Material.BARRIER, "§7Bloquear mobs normales", s.isBloquearMobsNormales(),
                "§7Solo aparecen: §f" + String.join(", ", s.getMobsPermitidos()),
                "§8(editable en config.yml -> ajustes-espacio.mobs)"));
        inv.setItem(SLOT_ESTRELLAS, toggle(Material.END_ROD, "§fPolvo estelar", s.isEstrellas(),
                "§7Particulas de estrellas alrededor del jugador."));
        inv.setItem(SLOT_TORMENTAS, toggle(Material.SUNFLOWER, "§6Tormentas solares", s.isTormentasSolares(),
                "§7Cada " + s.getTormentaIntervaloMin() + " min, radiacion durante " + s.getTormentaDuracionSeg() + "s.",
                "§7Protege: base o traje tier " + s.getTormentaTierProteccion() + "+."));

        // ---- MOBS: transformacion en vez de solo bloquear ----
        inv.setItem(SLOT_MOBS_TRANSFORMAR, toggle(Material.PHANTOM_MEMBRANE, "§5Transformar mobs normales",
                s.isTransformarMobsNormales(),
                "§7Al aparecer (natural, spawner, huevo o",
                "§7/summon), un mob que no este en la lista",
                "§7de permitidos se convierte en el mob",
                "§7especifico de abajo, en vez de bloquearse."));
        inv.setItem(SLOT_MOB_ESPACIAL, PlanetEditorGUI.item(Material.HUSK_SPAWN_EGG, "§5Mob especifico del espacio",
                "§7Ahora mismo: §f" + s.getMobEspacial(),
                "",
                "§eClic §7para rotar entre: §f" + String.join(", ", SpaceSettings.MOBS_ESPACIALES)));

        // ---- METEORITOS EN EL ESPACIO ABIERTO ----
        inv.setItem(SLOT_METEORITOS_ESPACIO, toggle(Material.NETHERRACK, "§cLluvia de meteoritos (espacio)",
                s.isMeteoritosEspacio(),
                "§7Cada cierto tiempo caen meteoritos",
                "§7cerca del jugador. No rompen bloques."));
        inv.setItem(SLOT_METEORITOS_ESPACIO_DANIO, valor(Material.MAGMA_CREAM, "§cDanio del meteorito (espacio)",
                String.format("%.1f", s.getDanioMeteoritoEspacio())));

        // ---- EL SOL: cerca = calor, lejos = frio ----
        boolean solMarcado = s.getSolX() != 0 || s.getSolY() != 200 || s.getSolZ() != 0 || s.isSolActivo();
        inv.setItem(SLOT_SOL_ACTIVO, toggle(Material.SUNFLOWER, "§6☀ Sol del espacio", s.isSolActivo(),
                solMarcado
                        ? "§7Posicion: §f" + (int) s.getSolX() + ", " + (int) s.getSolY() + ", " + (int) s.getSolZ()
                        : "§c✖ Todavia no se ha marcado con el hacha",
                "§7Cuanto mas cerca del sol en el espacio,",
                "§7mas calor; cuanto mas lejos, mas frio."));
        inv.setItem(SLOT_SOL_MARCAR, PlanetEditorGUI.item(Material.BLAZE_POWDER, "§6Marcar posicion del sol",
                "§7Recibe el hacha y activa el modo",
                "§7para fijar donde esta el sol.",
                "",
                "§eClic §7para recibir el hacha"));
        inv.setItem(SLOT_SOL_TEMP_CERCANA, valor(Material.BLAZE_ROD, "§6Temperatura junto al sol",
                String.format("%.0f°C", s.getSolTemperaturaCercana())));
        inv.setItem(SLOT_SOL_DISTANCIA, valor(Material.ENDER_EYE, "§6Distancia maxima del calor del sol",
                String.format("%.0f", s.getSolDistanciaMaxima()) + " §7bloques"));

        // ---- Submenus ----
        inv.setItem(SLOT_BASES, PlanetEditorGUI.item(Material.BEACON, "§a§lBases espaciales",
                "§7Zonas con oxigeno dentro del espacio.",
                "§7Bases: §f" + "ver menu", "", "§eClic §7para abrir"));
        inv.setItem(SLOT_TRAJES, PlanetEditorGUI.item(Material.LEATHER_HELMET, "§6§lTrajes espaciales",
                "§7Edita cascos y trajes de cada tier.", "", "§eClic §7para abrir"));
        inv.setItem(SLOT_HACHA, PlanetEditorGUI.item(Material.WOODEN_AXE, "§bHacha de seleccion",
                "§eClic §7para recibirla"));
        inv.setItem(SLOT_MAPA, PlanetEditorGUI.item(Material.ARROW, "§7« Mapa estelar"));

        jugador.openInventory(inv);
    }

    private ItemStack toggle(Material material, String nombre, boolean activo, String... descripcion) {
        List<String> lore = new ArrayList<>(Arrays.asList(descripcion));
        lore.add("");
        lore.add(activo ? "§a✔ ACTIVADO" : "§c✖ DESACTIVADO");
        lore.add("§eClic §7para cambiar");
        return PlanetEditorGUI.item(activo ? material : Material.GRAY_DYE, nombre, lore.toArray(new String[0]));
    }

    private ItemStack valor(Material material, String nombre, String valorActual) {
        return PlanetEditorGUI.item(material, nombre,
                "§7Valor: §f" + valorActual,
                "",
                "§eClic izq §7subir  §c| §eClic der §7bajar",
                "§8Shift = paso grande (x5)");
    }
}
