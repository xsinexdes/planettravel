package com.planettravel.commands;

import com.planettravel.PlanetTravel;
import com.planettravel.api.CoreNavesAPI;
import com.planettravel.api.Ship;
import com.planettravel.api.CoreNavesHook;
import com.planettravel.config.PlanetConfigManager;
import com.planettravel.config.PlanetData;
import com.planettravel.config.SuitTier;
import com.planettravel.gui.PlanetEditorGUI;
import com.planettravel.gui.PlanetInfo;
import com.planettravel.gui.StarMapGUI;
import com.planettravel.selection.AreaPlaneta;
import com.planettravel.suit.SuitManager;
import com.planettravel.util.Textos;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Implementa /planeta crear|editar|listar|teletransportar para poder configurar
 * todo sin tocar el config.yml a mano. Todos los cambios se aplican primero en
 * memoria (PlanetConfigManager) y se guardan a disco al final de cada operacion.
 */
public class PlanetCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMANDOS = Arrays.asList(
            "crear", "editar", "listar", "teletransportar", "gui", "borrar", "recargar", "validar",
            "area", "hacha", "traje", "info", "espacio");
    private static final List<String> CAMPOS_EDITABLES = Arrays.asList(
            "mundo", "centro", "centroaqui", "radio", "radiovisual", "entrada", "despegue",
            "spawnplaneta", "spawnaqui", "salidaespacio", "salidaaqui",
            "nombre", "descripcion", "descripcionadd", "descripcionborrar", "clasificacion", "icono",
            "tiercasco", "presion"
    );

    private final PlanetTravel plugin;
    private final PlanetConfigManager configManager;
    private final CoreNavesHook coreNavesHook;
    private final StarMapGUI starMapGUI;
    private final PlanetEditorGUI editorGUI;

    public PlanetCommand(PlanetTravel plugin, PlanetConfigManager configManager, CoreNavesHook coreNavesHook,
                         StarMapGUI starMapGUI, PlanetEditorGUI editorGUI) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.coreNavesHook = coreNavesHook;
        this.starMapGUI = starMapGUI;
        this.editorGUI = editorGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /planeta <crear|editar|listar|teletransportar|gui|borrar|recargar|validar|area|hacha|traje|info|espacio>");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "crear":
                return crear(sender, args);
            case "editar":
                return editar(sender, args);
            case "listar":
                return listar(sender);
            case "teletransportar":
                return teletransportar(sender, args);
            case "gui":
                return abrirGui(sender, args);
            case "borrar":
                return borrar(sender, args);
            case "recargar":
                return recargar(sender);
            case "validar":
                return validar(sender);
            case "area":
                return area(sender, args);
            case "hacha":
                return hacha(sender);
            case "traje":
                return traje(sender, args);
            case "info":
                return info(sender, args);
            case "espacio":
                return espacio(sender);
            default:
                sender.sendMessage("§cSubcomando desconocido. Usa: " + String.join(", ", SUBCOMANDOS) + ".");
                return true;
        }
    }

    // ---------------------------------------------------------------
    //  /planeta crear <id> <mundoPlaneta> <radio>
    //  El centro de la esfera en el mundo "espacio" se toma de la
    //  posicion actual del jugador que ejecuta el comando.
    // ---------------------------------------------------------------
    private boolean crear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede crear un planeta (se usa su posicion como centro).");
            return true;
        }
        if (args.length < 4) {
            sender.sendMessage("§cUso: /planeta crear <id> <mundoPlaneta> <radio>");
            return true;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        if (configManager.getPlaneta(id).isPresent()) {
            sender.sendMessage("§cYa existe un planeta con el id '" + id + "'.");
            return true;
        }

        String mundoPlaneta = args[2];
        double radio;
        try {
            radio = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cEl radio debe ser un numero.");
            return true;
        }

        Location aqui = jugador.getLocation();
        PlanetData datos = new PlanetData(id);
        datos.setWorldName(mundoPlaneta);
        datos.setEsferaEspacio(aqui.getX(), aqui.getY(), aqui.getZ(), radio);
        datos.setEntradaAtmosfericaY(configManager.getAlturaEntradaPorDefecto());
        datos.setAlturaDespegueY(configManager.getAlturaDespeguePorDefecto());

        // Valores por defecto razonables: el jugador los podra afinar despues con /planeta editar
        datos.setSpawnPlaneta(0, configManager.getAlturaEntradaPorDefecto(), 0, 0f, 0f);
        datos.setSalidaEspacio(aqui.getX() + radio + 50, aqui.getY(), aqui.getZ(), 0f, 0f);

        configManager.registrarPlaneta(datos);
        configManager.guardarTodo();

        sender.sendMessage("§a✔ Planeta '" + id + "' creado. Centro de la esfera: tu posicion actual.");
        sender.sendMessage("§7Siguientes pasos:");
        sender.sendMessage("§7 1. En el mundo del planeta: §f/planeta editar " + id + " spawnaqui");
        sender.sendMessage("§7 2. En el espacio, fuera de la esfera: §f/planeta editar " + id + " salidaaqui");
        sender.sendMessage("§7 3. Ajusta el entorno con §f/planeta gui " + id);
        mostrarAvisos(sender);
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta editar <id> <campo> <valores...>
    // ---------------------------------------------------------------
    private boolean editar(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUso: /planeta editar <id> <campo> [valores...]");
            sender.sendMessage("§7Campos: " + String.join(", ", CAMPOS_EDITABLES));
            return true;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        PlanetData datos = configManager.getPlaneta(id).orElse(null);
        if (datos == null) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + id + "'.");
            return true;
        }

        String campo = args[2].toLowerCase(Locale.ROOT);
        Player jugador = (sender instanceof Player) ? (Player) sender : null;

        try {
            switch (campo) {
                case "mundo":
                    requiereArgs(args, 4, "editar " + id + " mundo <nombreMundo>");
                    datos.setWorldName(args[3]);
                    break;

                case "centro":
                    requiereArgs(args, 6, "editar " + id + " centro <x> <y> <z>");
                    datos.setEsferaEspacio(d(args[3]), d(args[4]), d(args[5]), datos.getRadio());
                    break;

                case "centroaqui":
                    requiereJugador(jugador);
                    Location c = jugador.getLocation();
                    datos.setEsferaEspacio(c.getX(), c.getY(), c.getZ(), datos.getRadio());
                    break;

                case "radio":
                    requiereArgs(args, 4, "editar " + id + " radio <valor>");
                    datos.setEsferaEspacio(datos.getSpaceX(), datos.getSpaceY(), datos.getSpaceZ(), d(args[3]));
                    break;

                case "radiovisual":
                    // Radio de la esfera de BLOQUES construida con WorldEdit.
                    // Debe ser MENOR que el radio de deteccion, o las naves
                    // chocaran contra los bloques antes de que salte el TP.
                    requiereArgs(args, 4, "editar " + id + " radiovisual <valor>");
                    datos.setRadioVisual(d(args[3]));
                    break;

                case "nombre":
                    requiereArgs(args, 4, "editar " + id + " nombre <texto...>");
                    datos.setNombreVisible(unir(args, 3));
                    break;

                case "descripcion":
                    // Reemplaza toda la descripcion. Un '|' separa lineas. Admite &colores.
                    requiereArgs(args, 4, "editar " + id + " descripcion <texto... (usa | para separar lineas)>");
                    datos.setDescripcion(unir(args, 3));
                    break;

                case "descripcionadd":
                    requiereArgs(args, 4, "editar " + id + " descripcionadd <linea...>");
                    datos.anadirLineaDescripcion(unir(args, 3));
                    break;

                case "descripcionborrar":
                    datos.limpiarDescripcion();
                    break;

                case "clasificacion":
                    requiereArgs(args, 4, "editar " + id + " clasificacion <texto...>");
                    datos.setClasificacion(unir(args, 3));
                    break;

                case "tiercasco":
                    requiereArgs(args, 4, "editar " + id + " tiercasco <0-" + SuitTier.MAX_TIERS + ">  (0 = casco clasico)");
                    datos.getEntorno().setTierCasco((int) d(args[3]));
                    break;

                case "presion":
                    requiereArgs(args, 4, "editar " + id + " presion <atm>  (1 = Tierra, 0 = vacio)");
                    datos.getEntorno().setPresion(d(args[3]));
                    break;

                case "icono":
                    requiereArgs(args, 4, "editar " + id + " icono <MATERIAL>");
                    if (org.bukkit.Material.matchMaterial(args[3]) == null) {
                        throw new IllegalArgumentException("'" + args[3] + "' no es un material valido.");
                    }
                    datos.setIconoMaterial(args[3].toUpperCase(Locale.ROOT));
                    break;

                case "entrada":
                    requiereArgs(args, 4, "editar " + id + " entrada <y>");
                    datos.setEntradaAtmosfericaY(d(args[3]));
                    break;

                case "despegue":
                    requiereArgs(args, 4, "editar " + id + " despegue <y>");
                    datos.setAlturaDespegueY(d(args[3]));
                    break;

                case "spawnplaneta":
                    requiereArgs(args, 6, "editar " + id + " spawnplaneta <x> <y> <z> [yaw] [pitch]");
                    datos.setSpawnPlaneta(d(args[3]), d(args[4]), d(args[5]), f(args, 6), f(args, 7));
                    break;

                case "spawnaqui":
                    requiereJugador(jugador);
                    Location sp = jugador.getLocation();
                    datos.setSpawnPlaneta(sp.getX(), sp.getY(), sp.getZ(), sp.getYaw(), sp.getPitch());
                    break;

                case "salidaespacio":
                    requiereArgs(args, 6, "editar " + id + " salidaespacio <x> <y> <z> [yaw] [pitch]");
                    datos.setSalidaEspacio(d(args[3]), d(args[4]), d(args[5]), f(args, 6), f(args, 7));
                    break;

                case "salidaaqui":
                    requiereJugador(jugador);
                    Location se = jugador.getLocation();
                    datos.setSalidaEspacio(se.getX(), se.getY(), se.getZ(), se.getYaw(), se.getPitch());
                    break;

                default:
                    sender.sendMessage("§cCampo desconocido. Usa: " + String.join(", ", CAMPOS_EDITABLES));
                    return true;
            }
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("§c" + ex.getMessage());
            return true;
        }

        configManager.registrarPlaneta(datos);
        configManager.guardarTodo();
        sender.sendMessage("§aPlaneta '" + id + "' actualizado (" + campo + ").");

        // Tras cada edicion revisamos si la configuracion quedo en un estado
        // que provocaria bucles de teletransporte o colisiones.
        mostrarAvisos(sender);
        return true;
    }

    /** Une los argumentos desde 'desde' hasta el final en una sola cadena. */
    private static String unir(String[] args, int desde) {
        StringBuilder sb = new StringBuilder();
        for (int i = desde; i < args.length; i++) {
            if (i > desde) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    /** Muestra los avisos de validacion, si los hay. */
    private void mostrarAvisos(CommandSender sender) {
        List<String> avisos = configManager.validarPlanetas();
        for (String aviso : avisos) {
            sender.sendMessage("§e⚠ " + aviso);
        }
    }

    // ---------------------------------------------------------------
    //  /planeta gui [id]   -> mapa estelar, o editor de un planeta
    // ---------------------------------------------------------------
    private boolean abrirGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede abrir la interfaz.");
            return true;
        }

        if (args.length >= 2) {
            PlanetData datos = configManager.getPlaneta(args[1]).orElse(null);
            if (datos == null) {
                sender.sendMessage("§cNo existe ningun planeta con el id '" + args[1] + "'.");
                return true;
            }
            editorGUI.abrir(jugador, datos);
        } else {
            starMapGUI.abrir(jugador);
        }
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta borrar <id>
    // ---------------------------------------------------------------
    private boolean borrar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /planeta borrar <id>");
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        if (configManager.getPlaneta(id).isEmpty()) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + id + "'.");
            return true;
        }
        configManager.eliminarPlaneta(id);
        configManager.guardarTodo();
        sender.sendMessage("§aPlaneta '" + id + "' borrado de la configuracion.");
        sender.sendMessage("§7(El mundo en si no se ha tocado, solo su entrada aqui.)");
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta recargar
    // ---------------------------------------------------------------
    private boolean recargar(CommandSender sender) {
        configManager.cargar();
        plugin.getBaseManager().cargar();
        sender.sendMessage("§aConfiguracion recargada: §f" + configManager.getPlanetas().size() + " §aplaneta(s).");
        mostrarAvisos(sender);
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta validar
    // ---------------------------------------------------------------
    private boolean validar(CommandSender sender) {
        List<String> avisos = configManager.validarPlanetas();
        if (avisos.isEmpty()) {
            sender.sendMessage("§a✔ Todo correcto: no se han detectado problemas de configuracion.");
        } else {
            sender.sendMessage("§e§lSe han detectado " + avisos.size() + " problema(s):");
            for (String aviso : avisos) {
                sender.sendMessage("§e⚠ " + aviso);
            }
        }
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta listar
    // ---------------------------------------------------------------
    private boolean listar(CommandSender sender) {
        if (configManager.getPlanetas().isEmpty()) {
            sender.sendMessage("§7No hay ningun planeta configurado todavia.");
            return true;
        }

        sender.sendMessage("§b--- Planetas configurados (" + configManager.getPlanetas().size() + ") ---");
        for (PlanetData p : configManager.getPlanetas().values()) {
            sender.sendMessage(String.format(
                    "§f%s §7(%s) -> mundo=§f%s§7 centro=(%.0f, %.0f, %.0f) radio=%.0f entrada_y=%.0f despegue_y=%.0f",
                    p.getNombreVisible(), p.getId(), p.getWorldName(), p.getSpaceX(), p.getSpaceY(), p.getSpaceZ(),
                    p.getRadio(), p.getEntradaAtmosfericaY(), p.getAlturaDespegueY()
            ));
        }
        sender.sendMessage("§8Usa /planeta gui para la vista completa.");
        mostrarAvisos(sender);
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta teletransportar <id> [jugador]
    //  Teletransporte manual, pensado para pruebas y administracion:
    //  usa la misma via (CoreNavesAPI si el jugador pilota una nave,
    //  o simplemente al jugador) para llevarlo al spawn del planeta.
    // ---------------------------------------------------------------
    private boolean teletransportar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /planeta teletransportar <id> [jugador]");
            return true;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        PlanetData datos = configManager.getPlaneta(id).orElse(null);
        if (datos == null) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + id + "'.");
            return true;
        }

        Player objetivo;
        if (args.length >= 3) {
            objetivo = Bukkit.getPlayer(args[2]);
            if (objetivo == null) {
                sender.sendMessage("§cJugador '" + args[2] + "' no encontrado o desconectado.");
                return true;
            }
        } else if (sender instanceof Player) {
            objetivo = (Player) sender;
        } else {
            sender.sendMessage("§cDebes indicar un jugador si ejecutas esto desde consola.");
            return true;
        }

        var mundoPlaneta = com.planettravel.util.WorldUtil.cargarSiHaceFalta(
                datos.getWorldName(), Bukkit.getLogger());
        if (mundoPlaneta == null) {
            sender.sendMessage("§cNo se pudo cargar el mundo del planeta.");
            return true;
        }

        CoreNavesAPI api = coreNavesHook.getApi();
        if (api != null && api.estaPilotandoNave(objetivo)) {
            Ship nave = api.getNaveDelJugador(objetivo);
            api.teletransportarNave(nave, datos.getSpawnPlanetaLocation(mundoPlaneta), true);
        } else {
            objetivo.teleport(datos.getSpawnPlanetaLocation(mundoPlaneta));
        }

        sender.sendMessage("§aTeletransportado " + objetivo.getName() + " al planeta '" + id + "'.");
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta area <id> [we]
    //  Define el area del planeta con la seleccion del hacha (o de WorldEdit).
    // ---------------------------------------------------------------
    private boolean area(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede seleccionar un area.");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage("§cUso: /planeta area <id> [we]");
            sender.sendMessage("§7Marca la esfera del planeta con el hacha (/planeta hacha): clic izquierdo = pos 1,");
            sender.sendMessage("§7clic derecho = pos 2. Con 'we' se usa tu seleccion de WorldEdit.");
            return true;
        }
        PlanetData datos = configManager.getPlaneta(args[1]).orElse(null);
        if (datos == null) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + args[1] + "'.");
            return true;
        }
        boolean worldEdit = args.length >= 3 && args[2].equalsIgnoreCase("we");
        try {
            sender.sendMessage(AreaPlaneta.aplicarDesde(plugin, jugador, datos, worldEdit));
        } catch (IllegalStateException e) {
            sender.sendMessage("§c" + e.getMessage());
            return true;
        }
        mostrarAvisos(sender);
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta hacha
    // ---------------------------------------------------------------
    private boolean hacha(CommandSender sender) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede recibir el hacha.");
            return true;
        }
        jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
        jugador.sendMessage("§b✔ Hacha de seleccion entregada. §7Clic izquierdo = pos 1, clic derecho = pos 2.");
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta traje <tier> [jugador] [casco|pechera|pantalones|botas|completo]
    // ---------------------------------------------------------------
    private boolean traje(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /planeta traje <tier> [jugador] [casco|pechera|pantalones|botas|completo]");
            return true;
        }
        int tier;
        try {
            tier = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cEl tier debe ser un numero.");
            return true;
        }
        SuitTier definicion = configManager.getTraje(tier);
        if (definicion == null) {
            sender.sendMessage("§cNo existe el tier " + tier + " (hay del 1 al " + configManager.getMaxTierDefinido() + ").");
            return true;
        }

        Player objetivo = null;
        int siguiente = 2;
        if (args.length >= 3) {
            objetivo = Bukkit.getPlayerExact(args[2]);
            if (objetivo != null) {
                siguiente = 3;
            }
        }
        if (objetivo == null && sender instanceof Player p) {
            objetivo = p;
        }
        if (objetivo == null) {
            sender.sendMessage("§cIndica un jugador valido.");
            return true;
        }

        String pieza = (args.length > siguiente) ? args[siguiente].toLowerCase(Locale.ROOT) : "completo";
        SuitManager suits = plugin.getSuitManager();
        List<ItemStack> piezas = new ArrayList<>();
        switch (pieza) {
            case "casco" -> piezas.add(suits.crearPieza(definicion, "HELMET"));
            case "pechera" -> piezas.add(suits.crearPieza(definicion, "CHESTPLATE"));
            case "pantalones" -> piezas.add(suits.crearPieza(definicion, "LEGGINGS"));
            case "botas" -> piezas.add(suits.crearPieza(definicion, "BOOTS"));
            default -> piezas.addAll(suits.crearTrajeCompleto(definicion));
        }
        for (ItemStack item : piezas) {
            var sobrantes = objetivo.getInventory().addItem(item);
            for (ItemStack resto : sobrantes.values()) {
                objetivo.getWorld().dropItemNaturally(objetivo.getLocation(), resto);
            }
        }
        sender.sendMessage("§a✔ Entregado " + (piezas.size() == 1 ? "1 pieza" : piezas.size() + " piezas")
                + " del traje tier " + tier + " a " + objetivo.getName() + ".");
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta info <id>   -> ficha completa en el chat
    // ---------------------------------------------------------------
    private boolean info(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /planeta info <id>");
            return true;
        }
        PlanetData datos = configManager.getPlaneta(args[1]).orElse(null);
        if (datos == null) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + args[1] + "'.");
            return true;
        }
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§b§l" + datos.getNombreVisible()
                + (datos.getClasificacion().isEmpty() ? "" : " §8- §3" + Textos.color(datos.getClasificacion())));
        for (String linea : PlanetInfo.descripcion(datos)) {
            sender.sendMessage(linea);
        }
        for (String linea : PlanetInfo.ficha(datos, configManager)) {
            sender.sendMessage(linea);
        }
        sender.sendMessage("§8§m                                        ");
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta espacio   -> menu de configuracion del espacio
    // ---------------------------------------------------------------
    private boolean espacio(CommandSender sender) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede abrir el menu del espacio.");
            return true;
        }
        plugin.getSpaceMenuGUI().abrir(jugador);
        return true;
    }

    // ---------------------------------------------------------------
    //  Helpers de parseo/validacion
    // ---------------------------------------------------------------
    private static void requiereArgs(String[] args, int minimo, String uso) {
        if (args.length < minimo) {
            throw new IllegalArgumentException("Uso: /planeta " + uso);
        }
    }

    private static void requiereJugador(Player jugador) {
        if (jugador == null) {
            throw new IllegalArgumentException("Este campo solo se puede fijar desde el juego (usa tu posicion actual).");
        }
    }

    private static double d(String valor) {
        try {
            return Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + valor + "' no es un numero valido.");
        }
    }

    /** Lee un float opcional del array de args en la posicion 'index'; si no existe, devuelve 0. */
    private static float f(String[] args, int index) {
        if (index >= args.length) {
            return 0f;
        }
        try {
            return Float.parseFloat(args[index]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + args[index] + "' no es un numero valido.");
        }
    }

    // ---------------------------------------------------------------
    //  Autocompletado
    // ---------------------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filtrar(SUBCOMANDOS, args[0]);
        }

        if (args.length == 2 && Arrays.asList("editar", "teletransportar", "gui", "borrar", "area", "info")
                .contains(args[0].toLowerCase(Locale.ROOT))) {
            return filtrar(new ArrayList<>(configManager.getPlanetas().keySet()), args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("editar")) {
            return filtrar(CAMPOS_EDITABLES, args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("area")) {
            return filtrar(Arrays.asList("we"), args[2]);
        }

        if (args[0].equalsIgnoreCase("traje")) {
            if (args.length == 2) {
                List<String> tiers = new ArrayList<>();
                configManager.getTrajes().keySet().forEach(t -> tiers.add(String.valueOf(t)));
                return filtrar(tiers, args[1]);
            }
            if (args.length == 3) {
                List<String> opciones = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                opciones.addAll(Arrays.asList("casco", "pechera", "pantalones", "botas", "completo"));
                return filtrar(opciones, args[2]);
            }
            if (args.length == 4) {
                return filtrar(Arrays.asList("casco", "pechera", "pantalones", "botas", "completo"), args[3]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("teletransportar")) {
            return filtrar(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()), args[2]);
        }

        return new ArrayList<>();
    }

    private static List<String> filtrar(List<String> opciones, String escritoHastaAhora) {
        String prefijo = escritoHastaAhora.toLowerCase(Locale.ROOT);
        return opciones.stream()
                .filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefijo))
                .collect(Collectors.toList());
    }
}
