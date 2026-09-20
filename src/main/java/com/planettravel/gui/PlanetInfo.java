package com.planettravel.gui;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.PlanetEnvironment;
import com.planettravel.util.Textos;

import java.util.ArrayList;
import java.util.List;

/**
 * Genera la FICHA de un planeta (descripcion + datos fisicos + peligros +
 * equipo necesario). La usan el mapa estelar (lore) y /planetas <id> (chat),
 * asi que la informacion es identica en los dos sitios.
 */
public final class PlanetInfo {

    private PlanetInfo() {
    }

    /** Descripcion del planeta ya coloreada y ajustada a lineas de ~38 caracteres. */
    public static List<String> descripcion(PlanetData planeta) {
        List<String> lineas = new ArrayList<>();
        for (String bruta : planeta.getDescripcionLineas()) {
            for (String linea : Textos.envolver("§7" + Textos.color(bruta), 38)) {
                lineas.add(linea);
            }
        }
        return lineas;
    }

    /** Ficha tecnica: gravedad, atmosfera, temperatura, equipo y peligros. */
    public static List<String> ficha(PlanetData planeta, PlanetConfigManager config) {
        PlanetEnvironment env = planeta.getEntorno();
        List<String> l = new ArrayList<>();

        l.add("§6§lFicha planetaria");

        l.add("§7Gravedad: §f" + String.format("%.2f", env.getGravedad()) + "x §8" + etiquetaGravedad(env.getGravedad()));

        l.add("§7Atmosfera: §f" + env.getNombreAtmosfera() + " §8(" + String.format("%.2f", env.getPresion()) + " atm)");
        if (env.isRequiereCasco()) {
            l.add("§c ✖ No respirable");
        } else {
            l.add("§a ✔ Respirable");
        }

        if (env.isTemperaturaActiva()) {
            double medio = env.getBaseEfectiva();
            double osc = env.getVariacionEfectiva();
            l.add("§7Temperatura: §f" + String.format("%.0f°C", medio - osc)
                    + " §7a §f" + String.format("%.0f°C", medio + osc));
            l.add("§8 Zona segura: " + String.format("%.0f°C a %.0f°C", env.getUmbralFrio(), env.getUmbralCalor()));
        }

        if (env.isRequiereCasco()) {
            var suits = config.getTrajes();
            int req = Math.min(env.getTierCasco(), Math.max(1, config.getMaxTierDefinido()));
            if (env.getTierCasco() > 0 && suits.get(req) != null) {
                l.add("§7Equipo: §fTraje" + (env.isRequiereTrajeCompleto() ? " completo" : "")
                        + " tier " + req);
                l.add("§8 " + Textos.color(suits.get(req).getNombre()));
            } else {
                l.add("§7Equipo: §f" + Textos.legible(env.getMaterialCasco())
                        + (env.isRequiereTrajeCompleto() ? " + traje completo" : ""));
            }
        }
        if (env.getPresion() >= config.getPresionPeligrosa()) {
            l.add("§4 ⚠ Presion aplastante: §7necesitas traje completo con resistencia >= "
                    + String.format("%.0f atm", env.getPresion()));
        }

        if (env.isAguaToxica()) {
            l.add("§2 ☣ Agua corrosiva" + (env.isAguaEnvenena() ? " y venenosa" : ""));
        }
        if (env.isMeteoritos()) {
            l.add("§6 ☄ Lluvia de meteoritos");
        }
        if (env.isMobsConCasco()) {
            l.add("§7 ☠ Mobs con casco de vidrio");
        }
        if (!env.getEfectosPermanentes().isEmpty()) {
            l.add("§5 ✦ Efectos constantes:");
            for (var e : env.getEfectosPermanentes().entrySet()) {
                l.add("§8   • §7" + Textos.legible(e.getKey()) + " " + e.getValue());
            }
        }
        return l;
    }

    public static String etiquetaGravedad(double g) {
        if (g < 0.4) return "(lunar)";
        if (g < 0.9) return "(ligera)";
        if (g < 1.1) return "(terrestre)";
        if (g < 1.8) return "(pesada)";
        return "(aplastante)";
    }
}
