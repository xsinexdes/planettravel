package com.planettravel.util;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    /**
     * Lista TODOS los mundos que existen en la carpeta del servidor: tanto los
     * que ya estan cargados (Bukkit.getWorlds()) como las carpetas de mundo que
     * hay en disco pero que el servidor todavia no ha cargado. Asi el admin
     * puede elegir un mundo para un planeta sin tener que saberse el nombre
     * exacto ni cargarlo a mano primero.
     *
     * Una carpeta cuenta como "mundo" si contiene level.dat (el marcador
     * estandar de un mundo de Minecraft/Bukkit).
     */
    public static List<String> listarMundosDisponibles() {
        List<String> nombres = new ArrayList<>();

        for (World mundo : Bukkit.getWorlds()) {
            nombres.add(mundo.getName());
        }

        File contenedor = Bukkit.getWorldContainer();
        File[] carpetas = contenedor.listFiles(File::isDirectory);
        if (carpetas != null) {
            for (File carpeta : carpetas) {
                String nombre = carpeta.getName();
                if (nombres.contains(nombre)) {
                    continue; // ya esta cargado, no lo dupliques
                }
                if (esCarpetaDeMundo(carpeta)) {
                    nombres.add(nombre);
                }
            }
        }

        nombres.sort(Comparator.naturalOrder());
        return nombres;
    }

    private static boolean esCarpetaDeMundo(File carpeta) {
        if (new File(carpeta, "level.dat").isFile()) {
            return true;
        }
        // Formato tipico de un mundo Bukkit con dimensiones (the_nether/the_end
        // viven en subcarpetas DIM-1/DIM1 sin su propio level.dat).
        return new File(carpeta, "DIM-1").isDirectory() || new File(carpeta, "DIM1").isDirectory();
    }
}
