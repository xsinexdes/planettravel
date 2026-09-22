package com.planettravel.environment;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.space.EnvironmentResolver;
import com.planettravel.space.EnvironmentResolver.Contexto;
import com.planettravel.suit.SuitManager;
import com.planettravel.util.ChatUtil;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Tarea periodica que aplica el entorno a cada jugador segun DONDE esta:
 * un planeta, el espacio, o dentro de una base con oxigeno (ver {@link EnvironmentResolver}).
 *
 * Aplica, en este orden: aire/traje, presion, agua toxica, gravedad, efectos
 * permanentes, temperatura (con desgaste de armadura), clima/hora y un HUD en
 * el actionbar que resume el estado del jugador.
 *
 * RENDIMIENTO: corre una vez por segundo por defecto y descarta en el primer
 * paso a todo jugador que no este en un mundo regido por el plugin.
 */
public class EnvironmentManager extends BukkitRunnable {

    private final JavaPlugin plugin;
    private final PlanetConfigManager configManager;
    private final TemperatureManager temperatureManager;
    private final EnvironmentResolver resolver;
    private final SuitManager suits;

    /** Ultimo aviso por jugador y categoria ("uuid:aire"), para no spamear sonidos. */
    private final Map<String, Long> ultimoAviso = new HashMap<>();

    public EnvironmentManager(JavaPlugin plugin, PlanetConfigManager configManager,
                              TemperatureManager temperatureManager,
                              EnvironmentResolver resolver, SuitManager suits) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.temperatureManager = temperatureManager;
        this.resolver = resolver;
        this.suits = suits;
    }

    @Override
    public void run() {
        for (Player jugador : plugin.getServer().getOnlinePlayers()) {
            Contexto ctx = resolver.resolver(jugador.getLocation());

            // Descarte rapido: mundo ajeno a PlanetTravel (o espacio desactivado).
            if (ctx == null) {
                temperatureManager.limpiar(jugador.getUniqueId());
                continue;
            }

            GameMode modo = jugador.getGameMode();
            if (modo == GameMode.CREATIVE || modo == GameMode.SPECTATOR) {
                continue;
            }

            PlanetEnvironment env = ctx.env();
            List<String> alertas = new ArrayList<>();

            String estadoAire = aplicarAire(jugador, ctx, alertas);
            aplicarPresion(jugador, ctx, alertas);
            aplicarAgua(jugador, env);
            aplicarGravedad(jugador, ctx);
            if (!ctx.habitat()) {
                aplicarEfectosPermanentes(jugador, env);
            }
            double temperatura = aplicarTemperatura(jugador, ctx, alertas);
            aplicarClimaYHora(jugador.getWorld(), env);

            if (env.isRequiereCasco() || env.isTemperaturaActiva() || !alertas.isEmpty()) {
                mostrarHud(jugador, ctx, estadoAire, temperatura, alertas);
            }
        }
    }

    // ------------------------------------------------------------
    //  AIRE / TRAJE ESPACIAL
    // ------------------------------------------------------------

    /** Devuelve el fragmento de HUD del estado del aire. */
    private String aplicarAire(Player jugador, Contexto ctx, List<String> alertas) {
        if (ctx.hayOxigeno()) {
            return "§aO₂ ✔";
        }

        PlanetEnvironment env = ctx.env();
        double factor = suits.factorAsfixia(jugador, env);
        if (factor <= 0) {
            return "§bO₂ traje";
        }

        // Sin proteccion (o con un traje de tier insuficiente): asfixia progresiva.
        jugador.damage(env.getDanioSinCasco() * factor);
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 100, 0, true, false));
        if (factor >= 0.5) {
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, false));
        }

        if (factor < 1.0) {
            alertas.add("§cTraje insuficiente §7(necesitas §f" + suits.describirRequisito(env) + "§7)");
        } else {
            alertas.add("§4✖ Sin oxigeno §7(necesitas §f" + suits.describirRequisito(env) + "§7)");
        }
        if (avisar(jugador, "aire", 3000)) {
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1.0f, 0.6f);
        }
        return "§cO₂ ✖";
    }

    // ------------------------------------------------------------
    //  PRESION ATMOSFERICA
    // ------------------------------------------------------------

    /**
     * Una atmosfera aplastante hace danio salvo que lleves el traje COMPLETO y
     * su resistencia a presion (editable por tier) aguante la del planeta.
     */
    private void aplicarPresion(Player jugador, Contexto ctx, List<String> alertas) {
        if (ctx.habitat()) {
            return;
        }
        PlanetEnvironment env = ctx.env();
        double peligrosa = configManager.getPresionPeligrosa();
        if (env.getPresion() < peligrosa) {
            return;
        }
        if (suits.getResistenciaPresion(jugador) >= env.getPresion()) {
            return;
        }

        double factor = Math.min(4.0, env.getPresion() / peligrosa);
        jugador.damage(0.5 * factor * Math.max(1.0, env.getDanioSinCasco()));
        jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, false));
        alertas.add(String.format("§4⚠ Presion aplastante §7(%.1f atm)", env.getPresion()));

        if (avisar(jugador, "presion", 4000)) {
            jugador.playSound(jugador.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 1.0f, 0.4f);
        }
    }

    // ------------------------------------------------------------
    //  AGUA TOXICA
    // ------------------------------------------------------------

    private void aplicarAgua(Player jugador, PlanetEnvironment env) {
        if (!env.isAguaToxica()) {
            return;
        }

        Material bloque = jugador.getLocation().getBlock().getType();
        boolean enAgua = (bloque == Material.WATER) || jugador.isInWater();
        if (!enAgua) {
            return;
        }

        jugador.damage(env.getDanioAgua());

        if (env.isAguaEnvenena()) {
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 140, 0, true, true));
        }

        jugador.getWorld().spawnParticle(Particle.ITEM_SLIME, jugador.getLocation().add(0, 1, 0),
                8, 0.4, 0.6, 0.4, 0.01);

        if (avisar(jugador, "agua", 2000)) {
            ChatUtil.actionBar(jugador, "§2☣ §aEl agua de este lugar es corrosiva");
            jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 0.8f, 1.4f);
        }
    }

    // ------------------------------------------------------------
    //  GRAVEDAD
    // ------------------------------------------------------------

    /**
     * Minecraft no permite cambiar la gravedad de un jugador, asi que se simula:
     *   baja  -> salto potenciado + caida lenta
     *   alta  -> lentitud + empujon hacia abajo
     * Dentro de una base con gravedad artificial se siente 1.0 (normal).
     * (El danio por caida tambien se escala: ver SpaceProtectionListener.)
     */
    private void aplicarGravedad(Player jugador, Contexto ctx) {
        double g = ctx.gravedadEfectiva();

        if (g > 0.9 && g < 1.1) {
            return;
        }

        int duracion = 60;

        if (g < 0.9) {
            int nivelSalto = (int) Math.round((1.0 - g) * 4);
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST,
                    duracion, Math.max(0, nivelSalto), true, false));
            if (!jugador.isOnGround() && jugador.getVelocity().getY() < 0) {
                jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING,
                        duracion, 0, true, false));
            }
        } else {
            int nivel = (int) Math.round((g - 1.0) * 2);
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS,
                    duracion, Math.max(0, nivel), true, false));
            if (!jugador.isOnGround()) {
                jugador.setVelocity(jugador.getVelocity().add(new Vector(0, -0.08 * (g - 1.0), 0)));
            }
        }
    }

    // ------------------------------------------------------------
    //  EFECTOS PERMANENTES
    // ------------------------------------------------------------

    private void aplicarEfectosPermanentes(Player jugador, PlanetEnvironment env) {
        for (Map.Entry<String, Integer> entrada : env.getEfectosPermanentes().entrySet()) {
            PotionEffectType tipo = PotionEffectType.getByName(entrada.getKey());
            if (tipo == null) {
                continue;
            }
            int amplificador = Math.max(0, entrada.getValue() - 1);
            jugador.addPotionEffect(new PotionEffect(tipo, 60, amplificador, true, false));
        }
    }

    // ------------------------------------------------------------
    //  TEMPERATURA + ARMADURA
    // ------------------------------------------------------------

    /** Devuelve la temperatura del jugador, o NaN si el sistema no esta activo aqui. */
    private double aplicarTemperatura(Player jugador, Contexto ctx, List<String> alertas) {
        PlanetEnvironment env = ctx.env();
        if (!env.isTemperaturaActiva()) {
            return Double.NaN;
        }

        double temperatura = temperatureManager.actualizar(jugador, ctx);

        if (temperatura >= env.getUmbralCalor()) {
            // CALOR: danio escalado, hambre, humo. Nada de fuego: sin oxigeno no arde.
            double exceso = temperatura - env.getUmbralCalor();
            jugador.damage(env.getDanioTemperatura() * (1 + exceso / 20.0));
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, 100, 1, true, false));
            jugador.getWorld().spawnParticle(Particle.SMOKE,
                    jugador.getLocation().add(0, 1.8, 0), 4, 0.3, 0.3, 0.3, 0.01);

            if (avisar(jugador, "temp", 4000)) {
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_FIRE_AMBIENT, 0.6f, 1.2f);
            }
            if (env.isDesgasteArmadura() && desgastarArmadura(jugador, false, exceso)) {
                alertas.add("§6♨ Tu armadura se sobrecalienta");
            }

        } else if (temperatura <= env.getUmbralFrio()) {
            // FRIO: danio escalado, lentitud, y el jugador SE CONGELA (overlay de hielo).
            double exceso = env.getUmbralFrio() - temperatura;
            jugador.damage(env.getDanioTemperatura() * (1 + exceso / 20.0));
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 0, true, false));
            jugador.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, 100, 0, true, false));
            jugador.setFreezeTicks(Math.min(jugador.getMaxFreezeTicks(), jugador.getFreezeTicks() + 60));
            jugador.getWorld().spawnParticle(Particle.SNOWFLAKE,
                    jugador.getLocation().add(0, 1.8, 0), 6, 0.3, 0.3, 0.3, 0.01);

            if (avisar(jugador, "temp", 4000)) {
                jugador.playSound(jugador.getLocation(), Sound.BLOCK_POWDER_SNOW_STEP, 0.8f, 0.6f);
            }
            if (env.isDesgasteArmadura() && desgastarArmadura(jugador, true, exceso)) {
                alertas.add("§b❄ Tu armadura se congela y se agrieta");
            }
        }
        return temperatura;
    }

    /**
     * El frio/calor extremo DESGASTA la armadura:
     *   FRIO: el metal se vuelve quebradizo (desgaste completo), el cuero aguanta
     *         a medias y los trajes espaciales estan aislados (solo 1/4).
     *   CALOR: el metal se sobrecalienta, el netherite resiste del todo, los
     *         trajes espaciales solo sufren 1/4.
     * Nunca se rompe la pieza: se queda con 1 de durabilidad, para que el
     * castigo sea reparar, no perder el equipo de golpe.
     *
     * @return true si alguna pieza sufrio desgaste este ciclo
     */
    private boolean desgastarArmadura(Player jugador, boolean frio, double exceso) {
        double multiplicador = configManager.getDesgasteMultiplicador();
        if (multiplicador <= 0) {
            return false;
        }

        PlayerInventory inventario = jugador.getInventory();
        ItemStack[] piezas = inventario.getArmorContents();
        boolean hubo = false;

        for (int i = 0; i < piezas.length; i++) {
            ItemStack pieza = piezas[i];
            if (pieza == null || pieza.getType() == Material.AIR) {
                continue;
            }
            int maxima = pieza.getType().getMaxDurability();
            if (maxima <= 0) {
                continue;
            }

            String nombre = pieza.getType().name();
            double probabilidad;
            if (suits.getTier(pieza) > 0) {
                probabilidad = 0.25;
            } else if (frio) {
                probabilidad = nombre.startsWith("LEATHER_") ? 0.5 : 1.0;
            } else {
                probabilidad = nombre.startsWith("NETHERITE_") ? 0.0 : 1.0;
            }
            probabilidad = Math.min(1.0, probabilidad * multiplicador);
            if (probabilidad <= 0 || ThreadLocalRandom.current().nextDouble() >= probabilidad) {
                continue;
            }

            ItemMeta meta = pieza.getItemMeta();
            if (!(meta instanceof Damageable dano)) {
                continue;
            }
            int desgaste = 1 + (int) (exceso / 25.0);
            int nuevo = Math.min(maxima - 1, dano.getDamage() + desgaste);
            if (nuevo <= dano.getDamage()) {
                continue;
            }
            dano.setDamage(nuevo);
            pieza.setItemMeta(dano);
            piezas[i] = pieza;
            hubo = true;
        }

        if (hubo) {
            inventario.setArmorContents(piezas);
        }
        return hubo;
    }

    // ------------------------------------------------------------
    //  CLIMA Y HORA
    // ------------------------------------------------------------

    private void aplicarClimaYHora(World mundo, PlanetEnvironment env) {
        if (env.isBloquearClima()) {
            if (mundo.hasStorm() != env.isLluviaPermanente()) {
                mundo.setStorm(env.isLluviaPermanente());
            }
            mundo.setWeatherDuration(Integer.MAX_VALUE);
        }

        if (env.isBloquearHora()) {
            if (mundo.getTime() != env.getHoraFija()) {
                mundo.setTime(env.getHoraFija());
            }
        }
    }

    // ------------------------------------------------------------
    //  HUD
    // ------------------------------------------------------------

    /** Una sola linea de actionbar: aire | temperatura | tier de casco | zona | alertas. */
    private void mostrarHud(Player jugador, Contexto ctx, String aire, double temperatura, List<String> alertas) {
        StringBuilder sb = new StringBuilder(aire);

        if (!Double.isNaN(temperatura)) {
            sb.append(" §7| ")
                    .append(temperatureManager.colorPara(temperatura, ctx.env()))
                    .append(String.format("%.0f°C", temperatura));
        }

        int tier = suits.getTierCasco(jugador);
        if (tier > 0) {
            sb.append(" §7| §fT").append(tier);
        }

        sb.append(" §7| §f").append(ctx.zona());

        for (String alerta : alertas) {
            sb.append(" §7| ").append(alerta);
        }
        ChatUtil.actionBar(jugador, sb.toString());
    }

    // ------------------------------------------------------------

    /** Evita spamear sonidos: true si ya paso el intervalo para esa categoria. */
    private boolean avisar(Player jugador, String categoria, long intervaloMs) {
        String clave = jugador.getUniqueId() + ":" + categoria;
        long ahora = System.currentTimeMillis();
        Long anterior = ultimoAviso.get(clave);
        if (anterior != null && (ahora - anterior) < intervaloMs) {
            return false;
        }
        ultimoAviso.put(clave, ahora);
        return true;
    }

    public void limpiarJugador(UUID uuid) {
        String prefijo = uuid + ":";
        ultimoAviso.keySet().removeIf(k -> k.startsWith(prefijo));
        temperatureManager.limpiar(uuid);
    }
}
