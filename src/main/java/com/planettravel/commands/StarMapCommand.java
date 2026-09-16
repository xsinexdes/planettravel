package com.planettravel.commands;

import com.planettravel.gui.StarMapGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /planetas — abre el MAPA ESTELAR para cualquier jugador.
 *
 * A diferencia de /planeta (administracion), este comando es para todos:
 * muestra los planetas conocidos, su distancia y sus peligros, para que
 * el jugador pueda decidir a donde volar y con que equipo.
 */
public class StarMapCommand implements CommandExecutor {

    private final StarMapGUI starMapGUI;

    public StarMapCommand(StarMapGUI starMapGUI) {
        this.starMapGUI = starMapGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Solo un jugador puede abrir el mapa estelar.");
            return true;
        }
        starMapGUI.abrir(jugador);
        return true;
    }
}
