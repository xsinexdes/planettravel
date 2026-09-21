package com.planettravel.commands;

import com.planettravel.PlanetTravel;
import com.planettravel.selection.Seleccion;
import com.planettravel.space.SpaceBase;
import com.planettravel.util.Textos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * /espacio                       abre el menu de configuracion del espacio
 * /espacio hacha                 hacha de seleccion
 * /espacio base crear <id> [nombre...] [we]
 * /espacio base borrar|tp|listar|oxigeno|temperatura|redefinir ...
 * /espacio traje <tier> [jugador] [pieza]   (igual que /planeta traje)
 */
public class SpaceCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMANDOS = Arrays.asList("hacha", "base", "traje", "estado");
    private static final List<String> SUB_BASE =
            Arrays.asList("crear", "borrar", "tp", "listar", "oxigeno", "temperatura", "redefinir");

    private final PlanetTravel plugin;

    public SpaceCommand(PlanetTravel plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player jugador)) {
                sender.sendMessage("§cSolo un jugador puede abrir el menu. Usa: /espacio base listar");
                return true;
            }
            plugin.getSpaceMenuGUI().abrir(jugador);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "hacha" -> {
                if (sender instanceof Player jugador) {
                    jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
                    jugador.sendMessage("§b✔ Hacha de seleccion entregada. §7Clic izquierdo = pos 1, clic derecho = pos 2.");
                } else {
                    sender.sendMessage("§cSolo un jugador puede recibir el hacha.");
                }
            }
            case "base" -> base(sender, args);
            case "traje" -> {
                // Se reutiliza la logica de /planeta traje (args[0] ya es "traje")
                PluginCommand planeta = plugin.getCommand("planeta");
                if (planeta != null && planeta.getExecutor() != null) {
                    planeta.getExecutor().onCommand(sender, planeta, "planeta", args);
                }
            }
            case "estado" -> estado(sender);
            default -> sender.sendMessage("§cUso: /espacio [hacha|base|traje|estado]");
        }
        return true;
    }

    private void estado(CommandSender sender) {
        var s = plugin.getConfigManager().getSpaceSettings();
        sender.sendMessage("§b--- Espacio ---");
        sender.sendMessage("§7Reglas hostiles: " + (s.isActivo() ? "§aACTIVAS" : "§cdesactivadas") + " §8(interruptor en /espacio)");
        sender.sendMessage("§7Bases: §f" + plugin.getBaseManager().getBases().size()
                + " §7| Trajes (tiers): §f" + plugin.getConfigManager().getTrajes().size());
        sender.sendMessage("§7Sin fuego sin oxigeno: §f" + s.isProhibirFuegoSinOxigeno()
                + " §7| Mobs con casco: §f" + s.isMobsConCasco()
                + " §7| Tormentas solares: §f" + s.isTormentasSolares());
    }

    // ---------------------------------------------------------------
    //  /espacio base ...
    // ---------------------------------------------------------------
    private void base(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /espacio base <" + String.join("|", SUB_BASE) + "> ...");
            return;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);

        if (sub.equals("listar")) {
            sender.sendMessage("§b--- Bases espaciales (" + plugin.getBaseManager().getBases().size() + ") ---");
            for (SpaceBase b : plugin.getBaseManager().getBases()) {
                sender.sendMessage(String.format("§f%s §8(%s) §7mundo=§f%s §7%dx%dx%d oxigeno=%s temp=%.0f°C",
                        b.getNombre(), b.getId(), b.getMundo(), b.getAnchoX(), b.getAltoY(), b.getAnchoZ(),
                        b.isOxigeno() ? "§asi§7" : "§cno§7", b.getTemperatura()));
            }
            return;
        }

        if (args.length < 3) {
            sender.sendMessage("§cUso: /espacio base " + sub + " <id> ...");
            return;
        }
        String id = args[2];

        if (sub.equals("crear")) {
            crear(sender, args, id);
            return;
        }

        SpaceBase base = plugin.getBaseManager().get(id);
        if (base == null) {
            sender.sendMessage("§cNo existe ninguna base con el id '" + id + "'.");
            return;
        }

        switch (sub) {
            case "borrar" -> {
                plugin.getBaseManager().eliminar(base.getId());
                sender.sendMessage("§aBase '" + base.getId() + "' borrada.");
            }
            case "tp" -> {
                if (!(sender instanceof Player jugador)) {
                    sender.sendMessage("§cSolo un jugador puede teletransportarse.");
                    return;
                }
                World mundo = Bukkit.getWorld(base.getMundo());
                if (mundo == null) {
                    sender.sendMessage("§cEl mundo '" + base.getMundo() + "' no esta cargado.");
                    return;
                }
                jugador.teleport(new Location(mundo, base.getCentroX(), base.getMinY() + 1.0, base.getCentroZ()));
            }
            case "oxigeno" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUso: /espacio base oxigeno <id> <on|off>");
                    return;
                }
                base.setOxigeno(args[3].equalsIgnoreCase("on") || args[3].equalsIgnoreCase("true"));
                plugin.getBaseManager().guardar();
                sender.sendMessage("§aOxigeno de '" + base.getId() + "': " + (base.isOxigeno() ? "activado" : "apagado") + ".");
            }
            case "temperatura" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUso: /espacio base temperatura <id> <grados>");
                    return;
                }
                try {
                    base.setTemperatura(Double.parseDouble(args[3]));
                } catch (NumberFormatException e) {
                    sender.sendMessage("§c'" + args[3] + "' no es un numero.");
                    return;
                }
                plugin.getBaseManager().guardar();
                sender.sendMessage("§aTemperatura de '" + base.getId() + "': " + base.getTemperatura() + "°C.");
            }
            case "redefinir" -> {
                if (!(sender instanceof Player jugador)) {
                    sender.sendMessage("§cSolo un jugador puede seleccionar una zona.");
                    return;
                }
                try {
                    Seleccion sel = plugin.getSelectionManager().obtener(jugador, args.length >= 4 && args[3].equalsIgnoreCase("we"));
                    base.setMundo(sel.mundo().getName());
                    base.setArea(sel.minX(), sel.minY(), sel.minZ(), sel.maxX(), sel.maxY(), sel.maxZ());
                    plugin.getBaseManager().guardar();
                    sender.sendMessage("§aZona de '" + base.getId() + "' redefinida (" + sel.resumen() + ").");
                } catch (IllegalStateException e) {
                    sender.sendMessage("§c" + e.getMessage());
                }
            }
            default -> sender.sendMessage("§cSubcomando desconocido: " + sub);
        }
    }

    private void crear(CommandSender sender, String[] args, String id) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede crear una base (usa su seleccion).");
            return;
        }
        if (plugin.getBaseManager().get(id) != null) {
            sender.sendMessage("§cYa existe una base con ese id.");
            return;
        }

        // Palabras restantes = nombre; un 'we' final = usar la seleccion de WorldEdit
        boolean worldEdit = args.length > 3 && args[args.length - 1].equalsIgnoreCase("we");
        int fin = worldEdit ? args.length - 1 : args.length;
        String nombre = (fin > 3) ? String.join(" ", Arrays.copyOfRange(args, 3, fin)) : id;

        try {
            Seleccion sel = plugin.getSelectionManager().obtener(jugador, worldEdit);
            SpaceBase base = new SpaceBase(Textos.aId(id), Textos.color(nombre), sel.mundo().getName(),
                    sel.minX(), sel.minY(), sel.minZ(), sel.maxX(), sel.maxY(), sel.maxZ());
            plugin.getBaseManager().registrar(base);
            sender.sendMessage("§a✔ Base '" + base.getId() + "' creada (" + sel.resumen() + "). Dentro hay oxigeno.");
        } catch (IllegalStateException e) {
            sender.sendMessage("§c" + e.getMessage());
        }
    }

    // ---------------------------------------------------------------

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filtrar(SUBCOMANDOS, args[0]);
        }
        if (args[0].equalsIgnoreCase("base")) {
            if (args.length == 2) {
                return filtrar(SUB_BASE, args[1]);
            }
            if (args.length == 3 && !args[1].equalsIgnoreCase("crear") && !args[1].equalsIgnoreCase("listar")) {
                List<String> ids = plugin.getBaseManager().getBases().stream()
                        .map(SpaceBase::getId).collect(Collectors.toList());
                return filtrar(ids, args[2]);
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("oxigeno")) {
                return filtrar(Arrays.asList("on", "off"), args[3]);
            }
            if (args.length == 4 && args[1].equalsIgnoreCase("redefinir")) {
                return filtrar(Arrays.asList("we"), args[3]);
            }
        }
        if (args[0].equalsIgnoreCase("traje") && args.length == 2) {
            List<String> tiers = new ArrayList<>();
            plugin.getConfigManager().getTrajes().keySet().forEach(t -> tiers.add(String.valueOf(t)));
            return filtrar(tiers, args[1]);
        }
        return new ArrayList<>();
    }

    private static List<String> filtrar(List<String> opciones, String escrito) {
        String prefijo = escrito.toLowerCase(Locale.ROOT);
        return opciones.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefijo)).collect(Collectors.toList());
    }
}
