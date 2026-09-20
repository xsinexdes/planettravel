package com.planettravel.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.logging.Logger;

/**
 * Pequenia utilidad para obtener un mundo-planeta, cargandolo si hiciera falta.
 *
 * Con pocos planetas es perfectamente razonable mantenerlos SIEMPRE cargados
 * (llamando a cargarSiHaceFalta para cada uno en el onEnable del plugin).
 * Si en el futuro hay muchos mundos-planeta y no conviene tenerlos todos
 * cargados a la vez, esta misma funcion sirve para cargarlos "bajo demanda"
 * justo antes de teletransportar una nave hacia ellos.
 */
public final class WorldUtil {

    private WorldUtil() {
    }

    /**
     * Devuelve el World con ese nombre, cargandolo desde disco si Bukkit no
     * lo tiene ya en memoria. Nunca devuelve null salvo que el mundo no
     * exista realmente en la carpeta del servidor y no se pueda generar.
     */
    public static World cargarSiHaceFalta(String nombreMundo, Logger logger) {
        World mundo = Bukkit.getWorld(nombreMundo);
        if (mundo != null) {
            return mundo;
        }

        logger.info("Cargando mundo-planeta '" + nombreMundo + "' (no estaba cargado)...");
        WorldCreator creador = new WorldCreator(nombreMundo);
        mundo = creador.createWorld();

        if (mundo == null) {
            logger.severe("No se pudo cargar/crear el mundo '" + nombreMundo + "'. " +
                    "Revisa que el nombre en config.yml sea correcto.");
        }
        return mundo;
    }
}
