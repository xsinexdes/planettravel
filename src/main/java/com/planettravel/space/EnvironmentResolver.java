package com.planettravel.space;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.config.SpaceSettings;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Responde a la pregunta "que reglas rigen en ESTE punto?".
 *
 * Todos los sistemas (entorno, temperatura, fuego, mobs, meteoritos...) usan
 * este resolvedor en vez de averiguar por su cuenta si estas en un planeta, en
 * el espacio o dentro de una base. Asi hay UNA sola definicion de oxigeno.
 *
 *   punto en un mundo cualquiera  -> null (rige Minecraft normal)
 *   punto en un planeta           -> entorno del planeta
 *   punto en el espacio (activo)  -> entorno del espacio
 *   ... y si ademas esta dentro de una base con oxigeno -> habitat (aire + temperatura de la base)
 */
public class EnvironmentResolver {

    /** Resultado de la consulta. */
    public record Contexto(String zona, PlanetData planeta, boolean esEspacio,
                           PlanetEnvironment env, SpaceBase base,
                           boolean habitat, boolean hayOxigeno) {

        /** Gravedad que se siente aqui: 1.0 dentro de una base con gravedad artificial. */
        public double gravedadEfectiva() {
            return (habitat && base != null && base.isGravedadArtificial()) ? 1.0 : env.getGravedad();
        }
    }

    private final PlanetConfigManager config;
    private final SpaceBaseManager bases;

    public EnvironmentResolver(PlanetConfigManager config, SpaceBaseManager bases) {
        this.config = config;
        this.bases = bases;
    }

    /** Contexto en un punto, o null si ese mundo no esta regido por PlanetTravel. */
    public Contexto resolver(Location loc) {
        World mundo = loc.getWorld();
        if (mundo == null) {
            return null;
        }

        PlanetData planeta = null;
        PlanetEnvironment env;
        boolean espacio = false;
        String zona;

        if (mundo.getName().equals(config.getMundoEspacio())) {
            SpaceSettings ajustes = config.getSpaceSettings();
            if (!ajustes.isActivo()) {
                return null;
            }
            env = ajustes.getEntorno();
            espacio = true;
            zona = "Espacio";
        } else {
            planeta = config.getPlanetaPorMundo(mundo.getName());
            if (planeta == null) {
                return null;
            }
            env = planeta.getEntorno();
            zona = planeta.getNombreVisible();
        }

        SpaceBase base = bases.baseEn(loc);
        boolean habitat = base != null && base.tieneOxigeno();
        if (habitat) {
            zona = base.getNombre();
        }
        boolean oxigeno = habitat || !env.isRequiereCasco();

        return new Contexto(zona, planeta, espacio, env, base, habitat, oxigeno);
    }

    /** true si en ese punto se puede respirar (y por tanto hacer fuego). Mundos ajenos = true. */
    public boolean hayOxigeno(Location loc) {
        Contexto ctx = resolver(loc);
        return ctx == null || ctx.hayOxigeno();
    }
}
