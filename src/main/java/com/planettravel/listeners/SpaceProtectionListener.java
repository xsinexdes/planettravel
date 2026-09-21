package com.planettravel.listeners;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.SpaceSettings;
import com.planettravel.space.EnvironmentResolver;
import com.planettravel.space.EnvironmentResolver.Contexto;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.Set;

/**
 * Reglas fisicas del espacio y los planetas sin aire:
 *
 *  1) FUEGO: donde no hay oxigeno no se pueden colocar antorchas, fogatas ni
 *     velas, ni encender fuego, ni se propaga. (Las linternas, glowstone,
 *     sea lantern y lamparas de redstone SI valen: son luz electrica.)
 *  2) MOBS: en el espacio, cualquier mob "normal" que aparezca (de forma
 *     natural, desde un spawner, con huevo o con /summon) se TRANSFORMA en un
 *     mob especifico "adaptado al espacio" (configurable), en vez de solo
 *     bloquearse. Los mobs de la lista de permitidos aparecen tal cual.
 *  3) CAIDAS: el danio por caida se escala con la gravedad del lugar.
 */
public class SpaceProtectionListener implements Listener {

    /** Causas de aparicion que se vigilan: incluye spawns naturales, huevos Y /summon. */
    private static final Set<SpawnReason> RAZONES = EnumSet.of(
            SpawnReason.NATURAL, SpawnReason.CHUNK_GEN, SpawnReason.SPAWNER,
            SpawnReason.DEFAULT, SpawnReason.REINFORCEMENTS, SpawnReason.JOCKEY, SpawnReason.TRAP,
            SpawnReason.SPAWNER_EGG, SpawnReason.EGG, SpawnReason.DISPENSE_EGG,
            SpawnReason.COMMAND, SpawnReason.CUSTOM);

    /** Mobs que muestran el casco en la cabeza (por nombre, para no depender de constantes nuevas). */
    private static final Set<String> HUMANOIDES = Set.of(
            "ZOMBIE", "HUSK", "DROWNED", "ZOMBIE_VILLAGER", "ZOMBIFIED_PIGLIN",
            "SKELETON", "STRAY", "BOGGED", "PIGLIN", "PIGLIN_BRUTE");

    private final Plugin plugin;
    private final PlanetConfigManager config;
    private final EnvironmentResolver resolver;

    public SpaceProtectionListener(Plugin plugin, PlanetConfigManager config, EnvironmentResolver resolver) {
        this.plugin = plugin;
        this.config = config;
        this.resolver = resolver;
    }

    // ------------------------------------------------------------
    //  FUEGO
    // ------------------------------------------------------------

    private boolean esFuente(Material m) {
        return m == Material.TORCH || m == Material.WALL_TORCH
                || m == Material.SOUL_TORCH || m == Material.SOUL_WALL_TORCH
                || m == Material.FIRE || m == Material.SOUL_FIRE
                || Tag.CAMPFIRES.isTagged(m) || Tag.CANDLES.isTagged(m);
    }

    private boolean fuegoBloqueado(org.bukkit.Location loc) {
        if (!config.getSpaceSettings().isProhibirFuegoSinOxigeno()) {
            return false;
        }
        return !resolver.hayOxigeno(loc);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (!esFuente(event.getBlockPlaced().getType())) {
            return;
        }
        if (fuegoBloqueado(event.getBlockPlaced().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("§c✖ Sin oxigeno no hay fuego. §7Usa luz electrica: linternas, glowstone o lamparas de redstone.");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        if (fuegoBloqueado(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (fuegoBloqueado(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getSource().getType() == Material.FIRE && fuegoBloqueado(event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    // ------------------------------------------------------------
    //  MOBS
    // ------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        if (!RAZONES.contains(event.getSpawnReason())) {
            return;
        }
        LivingEntity mob = event.getEntity();
        World mundo = mob.getWorld();

        if (mundo.getName().equals(config.getMundoEspacio())) {
            SpaceSettings espacio = config.getSpaceSettings();
            if (!espacio.isActivo()) {
                return;
            }
            String tipo = mob.getType().name();
            boolean permitido = espacio.getMobsPermitidos().contains(tipo)
                    || tipo.equals(espacio.getMobEspacial()); // el mob espacial nunca se bloquea/transforma a si mismo
            if (!permitido) {
                if (espacio.isTransformarMobsNormales()) {
                    // En vez de bloquear sin mas: cancelamos ESTE mob "normal" y
                    // en su lugar aparece el mob especifico del espacio, en el
                    // mismo sitio, al tick siguiente (no se puede cambiar el tipo
                    // de una entidad ya creada).
                    event.setCancelled(true);
                    transformarEnMobEspacial(mob, espacio);
                    return;
                }
                if (espacio.isBloquearMobsNormales()) {
                    event.setCancelled(true);
                    return;
                }
            }
            if (espacio.isMobsConCasco()) {
                ponerCasco(mob, espacio.getMaterialCascoMobs(), espacio.getProbabilidadSoltarCasco());
            }
            return;
        }

        PlanetData planeta = config.getPlanetaPorMundo(mundo.getName());
        if (planeta != null && planeta.getEntorno().isMobsConCasco()) {
            ponerCasco(mob, config.getSpaceSettings().getMaterialCascoMobs(),
                    config.getSpaceSettings().getProbabilidadSoltarCasco());
        }
    }

    /** Sustituye un mob "normal" recien cancelado por el mob especifico configurado para el espacio. */
    private void transformarEnMobEspacial(LivingEntity original, SpaceSettings espacio) {
        EntityType tipoNuevo;
        try {
            tipoNuevo = EntityType.valueOf(espacio.getMobEspacial().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return; // nombre invalido en config: no rompemos nada, simplemente no aparece nada
        }
        org.bukkit.Location loc = original.getLocation();
        World mundo = original.getWorld();
        // Usamos el overload mas simple (sin lambda/Consumer) a proposito: con
        // varias sobrecargas de spawnEntity que aceptan una lambda, javac
        // puede quedarse sin saber cual usar (ambiguo), igual que paso con
        // runTask. Configuramos el casco DESPUES, con una llamada normal.
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!mundo.isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) {
                return;
            }
            org.bukkit.entity.Entity nueva = mundo.spawnEntity(loc, tipoNuevo);
            if (nueva instanceof LivingEntity vivo && espacio.isMobsConCasco()) {
                ponerCasco(vivo, espacio.getMaterialCascoMobs(), espacio.getProbabilidadSoltarCasco());
            }
        });
    }

    private void ponerCasco(LivingEntity mob, String material, double probabilidadSoltar) {
        if (!HUMANOIDES.contains(mob.getType().name())) {
            return;
        }
        Material m = Material.matchMaterial(material);
        EntityEquipment equipo = mob.getEquipment();
        if (m == null || equipo == null) {
            return;
        }
        ItemStack actual = equipo.getHelmet();
        if (actual != null && actual.getType() != Material.AIR) {
            return;
        }
        equipo.setHelmet(new ItemStack(m));
        equipo.setHelmetDropChance((float) probabilidadSoltar);
    }

    // ------------------------------------------------------------
    //  CAIDAS
    // ------------------------------------------------------------

    @EventHandler(ignoreCancelled = true)
    public void onCaida(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        if (!(event.getEntity() instanceof Player jugador)) {
            return;
        }
        Contexto ctx = resolver.resolver(jugador.getLocation());
        if (ctx == null) {
            return;
        }
        double g = ctx.gravedadEfectiva();
        if (Math.abs(g - 1.0) < 0.05) {
            return;
        }
        event.setDamage(event.getDamage() * g);
    }
}
