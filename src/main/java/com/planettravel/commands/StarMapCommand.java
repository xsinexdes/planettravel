package com.planettravel.commands;

import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.gui.PlanetInfo;
import com.planettravel.gui.StarMapGUI;
import com.planettravel.util.Textos;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /planetas — abre el MAPA ESTELAR para cualquier jugador.
 * /planetas <id> — muestra en el chat la FICHA completa de un planeta
 * (descripcion, gravedad, atmosfera, temperatura, equipo necesario y peligros).
 *
 * A diferencia de /planeta (administracion), este comando es para todos.
 */
public class StarMapCommand implements CommandExecutor {

    private final StarMapGUI starMapGUI;
    private final PlanetConfigManager configManager;

    public StarMapCommand(StarMapGUI starMapGUI, PlanetConfigManager configManager) {
        this.starMapGUI = starMapGUI;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            PlanetData planeta = configManager.getPlaneta(args[0]).orElse(null);
            if (planeta == null) {
                sender.sendMessage("§cNo existe ningun planeta con el id '" + args[0] + "'.");
                return true;
            }
            sender.sendMessage("§8§m                                        ");
            sender.sendMessage("§b§l" + planeta.getNombreVisible()
                    + (planeta.getClasificacion().isEmpty() ? "" : " §8- §3" + Textos.color(planeta.getClasificacion())));
            for (String linea : PlanetInfo.descripcion(planeta)) {
                sender.sendMessage(linea);
            }
            for (String linea : PlanetInfo.ficha(planeta, configManager)) {
                sender.sendMessage(linea);
            }
            sender.sendMessage("§8§m                                        ");
            return true;
        }

        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("Solo un jugador puede abrir el mapa estelar.");
            return true;
        }
        starMapGUI.abrir(jugador);
        return true;
    }
}
