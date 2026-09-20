package com.planettravel.api;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Contrato PUBLICO que PlanetTravel espera encontrar en el plugin CoreNaves
 * (que a fecha de hoy todavia no existe). Cuando CoreNaves se implemente, debe:
 *
 *   1) Exponer una clase que implemente esta interfaz (o un adaptador equivalente).
 *   2) Registrarla en el ServicesManager de Bukkit en su onEnable(), por ejemplo:
 *
 *        Bukkit.getServicesManager().register(
 *            CoreNavesAPI.class, miImplementacion, this, ServicePriority.Normal);
 *
 *      (PlanetTravel busca la implementacion exactamente asi, ver CoreNavesHook).
 *
 * Todos los metodos reciben/devuelven tipos estandar de Bukkit para no atar
 * PlanetTravel a ninguna clase interna de CoreNaves.
 */
public interface CoreNavesAPI {

    /** true si el jugador esta AHORA MISMO pilotando alguna nave (en cualquier mundo). */
    boolean estaPilotandoNave(Player player);

    /** Devuelve la nave que el jugador esta pilotando, o null si no pilota ninguna. */
    Ship getNaveDelJugador(Player player);

    /**
     * Teletransporta la nave COMPLETA (todos sus bloques/entidades/pasajeros) al
     * destino indicado, conservando su orientacion (yaw/pitch) relativa de vuelo.
     *
     * @param nave               la nave a mover (obtenida con getNaveDelJugador)
     * @param destino            Location de destino, en el mundo correspondiente
     * @param mantenerOrientacion si true, la rotacion actual de la nave se preserva
     *                            y solo se aplica el yaw/pitch de destino como
     *                            referencia adicional (segun implemente CoreNaves)
     */
    void teletransportarNave(Ship nave, Location destino, boolean mantenerOrientacion);
}
