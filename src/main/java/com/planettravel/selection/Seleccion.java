package com.planettravel.selection;

import org.bukkit.World;

/** Region cuboide seleccionada (limites inclusivos, en coordenadas de bloque). */
public record Seleccion(World mundo, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

    public static Seleccion de(World mundo, int x1, int y1, int z1, int x2, int y2, int z2) {
        return new Seleccion(mundo,
                Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public int anchoX() { return maxX - minX + 1; }
    public int altoY() { return maxY - minY + 1; }
    public int anchoZ() { return maxZ - minZ + 1; }

    public long volumen() { return (long) anchoX() * altoY() * anchoZ(); }

    public String resumen() {
        return anchoX() + "x" + altoY() + "x" + anchoZ() + " bloques";
    }
}
