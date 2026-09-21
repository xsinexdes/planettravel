package com.planettravel.selection;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.InvocationTargetException;

/**
 * Lee la seleccion que el jugador tenga en WorldEdit / FastAsyncWorldEdit SIN
 * depender de su API en tiempo de compilacion (por reflexion). Asi PlanetTravel
 * compila y arranca igual con o sin WorldEdit instalado.
 */
public final class WorldEditBridge {

    private WorldEditBridge() {
    }

    public static Seleccion obtenerSeleccion(Player jugador) {
        Plugin we = Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (we == null) {
            we = Bukkit.getPluginManager().getPlugin("WorldEdit");
        }
        if (we == null) {
            throw new IllegalStateException("WorldEdit / FastAsyncWorldEdit no esta instalado.");
        }

        try {
            World mundo = jugador.getWorld();

            Object sesion = we.getClass().getMethod("getSession", Player.class).invoke(we, jugador);

            Class<?> adaptador = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            Object mundoWE = adaptador.getMethod("adapt", World.class).invoke(null, mundo);

            Class<?> claseMundoWE = Class.forName("com.sk89q.worldedit.world.World");
            Object region = sesion.getClass().getMethod("getSelection", claseMundoWE).invoke(sesion, mundoWE);

            Class<?> claseRegion = Class.forName("com.sk89q.worldedit.regions.Region");
            Object min = claseRegion.getMethod("getMinimumPoint").invoke(region);
            Object max = claseRegion.getMethod("getMaximumPoint").invoke(region);

            Class<?> vector = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            int minX = entero(vector, min, "getX");
            int minY = entero(vector, min, "getY");
            int minZ = entero(vector, min, "getZ");
            int maxX = entero(vector, max, "getX");
            int maxY = entero(vector, max, "getY");
            int maxZ = entero(vector, max, "getZ");

            return Seleccion.de(mundo, minX, minY, minZ, maxX, maxY, maxZ);

        } catch (InvocationTargetException e) {
            // WorldEdit lanza IncompleteRegionException si falta algun punto.
            throw new IllegalStateException("No tienes una seleccion completa en WorldEdit (//pos1 y //pos2).");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("No se pudo leer la seleccion de WorldEdit (" + e.getClass().getSimpleName()
                    + "). Usa el hacha de PlanetTravel: /espacio hacha.");
        }
    }

    private static int entero(Class<?> clase, Object objeto, String metodo) throws ReflectiveOperationException {
        return ((Number) clase.getMethod(metodo).invoke(objeto)).intValue();
    }
}
