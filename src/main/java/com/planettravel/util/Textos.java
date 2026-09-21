package com.planettravel.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilidades de texto: colores con '&' y ajuste de lineas largas para lores.
 */
public final class Textos {

    private Textos() {
    }

    /** Convierte los codigos '&' en codigos de color de Minecraft ('§'). */
    public static String color(String texto) {
        if (texto == null) {
            return "";
        }
        return ChatColor.translateAlternateColorCodes('&', texto);
    }

    /** Largo del texto sin contar los codigos de color. */
    public static int largoVisible(String s) {
        return s.replaceAll("§.", "").length();
    }

    /**
     * Parte un texto largo en lineas de como maximo 'ancho' caracteres visibles.
     * El ultimo color activo se arrastra a la linea siguiente, porque en un lore
     * cada linea empieza "limpia" y sin esto el color se perderia.
     */
    public static List<String> envolver(String texto, int ancho) {
        List<String> lineas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return lineas;
        }

        StringBuilder actual = new StringBuilder();
        int largoActual = 0;
        String colorActivo = "";

        for (String palabra : texto.split(" ")) {
            int largoPalabra = largoVisible(palabra);

            if (largoActual > 0 && largoActual + 1 + largoPalabra > ancho) {
                lineas.add(actual.toString());
                actual = new StringBuilder(colorActivo);
                largoActual = 0;
            }
            if (largoActual > 0) {
                actual.append(' ');
                largoActual++;
            }
            actual.append(palabra);
            largoActual += largoPalabra;
            colorActivo = ultimoColor(palabra, colorActivo);
        }

        if (largoActual > 0) {
            lineas.add(actual.toString());
        }
        return lineas;
    }

    private static String ultimoColor(String s, String previo) {
        String resultado = previo;
        for (int i = 0; i < s.length() - 1; i++) {
            if (s.charAt(i) == '§') {
                char c = Character.toLowerCase(s.charAt(i + 1));
                if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')) {
                    resultado = "§" + c;
                } else if (c == 'r') {
                    resultado = "";
                }
            }
        }
        return resultado;
    }

    /** SNAKE_CASE -> "Snake case". */
    public static String legible(String nombre) {
        if (nombre == null || nombre.isEmpty()) {
            return "";
        }
        String limpio = nombre.toLowerCase().replace('_', ' ');
        return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
    }

    /** Convierte un nombre libre en un id simple: minusculas, sin espacios ni simbolos. */
    public static String aId(String nombre) {
        String base = nombre.toLowerCase().trim()
                .replace('á', 'a').replace('é', 'e').replace('í', 'i')
                .replace('ó', 'o').replace('ú', 'u').replace('ñ', 'n');
        base = base.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        return base.isEmpty() ? "base" : base;
    }
}
