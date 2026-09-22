package com.planettravel.api.fallback;

import com.planettravel.api.CoreNavesAPI;
import com.planettravel.api.Ship;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Implementacion de emergencia de {@link CoreNavesAPI} usada mientras el
 * plugin CoreNaves no este instalado en el servidor.
 *
 * "Pilotar una nave" se simula asi: el jugador esta pilotando si tiene el
 * item marcador configurado en la mano (por defecto una elytra... en
 * realidad, para simplificar, aqui consideramos que CUALQUIER jugador que
 * este volando/cayendo libremente en el mundo "espacio" o en un mundo-planeta
 * "pilota" al efecto de poder probar aterrizajes y despegues sin bloquear
 * el desarrollo del resto del plugin a la espera de CoreNaves.
 *
 * IMPORTANTE: esto es solo para pruebas. En cuanto exista CoreNaves de verdad,
 * {@link com.planettravel.api.CoreNavesHook} usara su implementacion real y
 * esta clase deja de intervenir.
 */
public class CoreNavesAPIFallback implements CoreNavesAPI {

    @Override
    public boolean estaPilotandoNave(Player player) {
        // Modo de pruebas: tratamos al propio jugador como si fuera la nave.
        // Sustituir esta condicion por la logica real de CoreNaves en cuanto exista.
        return player.isOnline();
    }

    @Override
    public Ship getNaveDelJugador(Player player) {
        if (!estaPilotandoNave(player)) {
            return null;
        }
        return new JugadorComoNave(player);
    }

    @Override
    public void teletransportarNave(Ship nave, Location destino, boolean mantenerOrientacion) {
        Player piloto = nave.getPiloto();
        if (piloto == null || !piloto.isOnline()) {
            return;
        }
        // Sin CoreNaves solo podemos mover al jugador, no un conjunto de bloques.
        piloto.teleport(destino);
    }
}
