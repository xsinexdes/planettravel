package com.planettravel.api.fallback;

import com.planettravel.api.Ship;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.UUID;

/**
 * Implementacion "de mentira" de Ship usada SOLO cuando CoreNaves no esta
 * instalado (modo-sin-corenaves: true en config.yml). En vez de mover una
 * nave real, simplemente envuelve al jugador y lo trata como si el mismo
 * fuera la nave: sirve para poder probar toda la logica de aterrizaje /
 * despegue / comandos mientras el otro plugin no existe todavia.
 *
 * En cuanto CoreNaves este disponible, esta clase deja de usarse por
 * completo: {@link com.planettravel.api.CoreNavesHook} preferira siempre
 * la implementacion real registrada en el ServicesManager.
 */
public class JugadorComoNave implements Ship {

    private final Player jugador;

    public JugadorComoNave(Player jugador) {
        this.jugador = jugador;
    }

    @Override
    public UUID getId() {
        return jugador.getUniqueId();
    }

    @Override
    public Location getLocation() {
        return jugador.getLocation();
    }

    @Override
    public Vector getVelocity() {
        return jugador.getVelocity();
    }

    @Override
    public Player getPiloto() {
        return jugador;
    }

    @Override
    public boolean isPilotada() {
        return jugador.isOnline();
    }
}
