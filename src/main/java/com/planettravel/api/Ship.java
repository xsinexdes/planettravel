package com.planettravel.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Representa una nave completa (el conjunto de bloques/entidades que CoreNaves
 * mueve como una unidad), independientemente de como este implementada por dentro.
 *
 * IMPORTANTE: esta interfaz es el "contrato" que PlanetTravel espera de CoreNaves.
 * Cuando el plugin CoreNaves exista de verdad, su implementacion de Ship (o un
 * adaptador que la envuelva) es la que debe registrarse via {@link CoreNavesAPI}.
 * Mientras tanto, en modo-sin-corenaves usamos {@link com.planettravel.api.fallback.JugadorComoNave}
 * como implementacion de prueba que solo mueve al jugador.
 */
public interface Ship {

    /** Identificador unico de la nave (para cooldowns, logs, etc). */
    UUID getId();

    /** Ubicacion "de referencia" de la nave (normalmente su centro o su timon). */
    Location getLocation();

    /** Velocidad actual de la nave, para poder calcular danio por friccion al aterrizar fuerte. */
    Vector getVelocity();

    /** El jugador que la esta pilotando en este momento (nunca null si isPilotada() es true). */
    Player getPiloto();

    /** true si la nave tiene actualmente un piloto activo (controlando el vuelo). */
    boolean isPilotada();
}
