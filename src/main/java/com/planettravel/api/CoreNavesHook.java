package com.planettravel.api;

import com.planettravel.api.fallback.CoreNavesAPIFallback;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Punto unico por el que TODO el resto del plugin obtiene la {@link CoreNavesAPI}.
 * Nunca instancies CoreNavesAPIFallback ni busques el plugin "CoreNaves" a mano
 * en otro sitio: usa siempre {@link #getApi()}.
 *
 * Funcionamiento:
 *  - Si el plugin "CoreNaves" esta instalado y ha registrado su implementacion
 *    de CoreNavesAPI en el ServicesManager de Bukkit, la usamos directamente.
 *  - Si no, y modo-sin-corenaves=true en config.yml, usamos CoreNavesAPIFallback
 *    (mueve solo al jugador) para poder seguir probando el plugin.
 *  - Si no, y modo-sin-corenaves=false, getApi() devuelve null y quien lo llame
 *    debe avisar al jugador/consola de que falta CoreNaves.
 */
public class CoreNavesHook {

    private final JavaPlugin plugin;
    private final boolean modoSinCoreNaves;
    private CoreNavesAPI apiCacheada;

    public CoreNavesHook(JavaPlugin plugin, boolean modoSinCoreNaves) {
        this.plugin = plugin;
        this.modoSinCoreNaves = modoSinCoreNaves;
    }

    /**
     * Intenta enganchar con CoreNaves. Se llama una vez en onEnable() y de nuevo
     * si algun comando de recarga lo pide; el resultado se cachea en {@link #apiCacheada}.
     */
    public void enganchar() {
        RegisteredServiceProvider<CoreNavesAPI> registro =
                Bukkit.getServicesManager().getRegistration(CoreNavesAPI.class);

        if (registro != null) {
            this.apiCacheada = registro.getProvider();
            plugin.getLogger().info("CoreNaves detectado: usando su API real para mover naves.");
            return;
        }

        if (modoSinCoreNaves) {
            plugin.getLogger().warning("CoreNaves no esta instalado. PlanetTravel arranca en " +
                    "MODO SIN CORENAVES (solo se movera al jugador, no naves completas).");
            this.apiCacheada = new CoreNavesAPIFallback();
        } else {
            plugin.getLogger().severe("CoreNaves no esta instalado y 'modo-sin-corenaves' esta " +
                    "en false. PlanetTravel no podra teletransportar nada hasta que instales CoreNaves " +
                    "o actives el modo sin CoreNaves en config.yml.");
            this.apiCacheada = null;
        }
    }

    /** @return la API activa (real o de respaldo), o null si no hay ninguna disponible. */
    public CoreNavesAPI getApi() {
        return apiCacheada;
    }

    /** true si la API activa es la implementacion de pruebas y no CoreNaves real. */
    public boolean isModoRespaldo() {
        return apiCacheada instanceof CoreNavesAPIFallback;
    }
}
