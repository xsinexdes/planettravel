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
import com.planettravel.selection.SelectionManager.TipoPunto;
import com.planettravel.suit.SuitManager;
import com.planettravel.util.Textos;
import com.planettravel.util.WorldUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
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
            "area", "hacha", "traje", "info", "espacio", "mundos");
    private static final List<String> CAMPOS_EDITABLES = Arrays.asList(
            "mundo", "centro", "centroaqui", "radio", "radiovisual", "entrada", "despegue",
            "entradaworldborder", "deteccioncaja",
            "nombre", "descripcion", "descripcionadd", "descripcionborrar", "clasificacion", "icono",
            "tiercasco", "presion"
    );
    // Nota: el punto de ENTRADA (aterrizaje en el planeta) y el de SALIDA
    // (despegue al espacio) ya NO se editan con coordenadas ni "aqui": se
    // marcan siempre con el hacha de seleccion via /planeta hacha entrada|salida <id>.
    // Es la unica forma de fijarlos, a proposito.

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

    private void entregarHachaEnModo(Player jugador, TipoPunto tipo, PlanetData planeta) {
        jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
        plugin.getSelectionManager().activarModoPunto(jugador, tipo, planeta.getId());
        String mundoObjetivo = (tipo == TipoPunto.ENTRADA) ? planeta.getWorldName() : configManager.getMundoEspacio();
        String queEs = (tipo == TipoPunto.ENTRADA) ? "ENTRADA (donde aterrizan las naves)"
                : "SALIDA (donde aparecen en el espacio al despegar)";
        jugador.sendMessage("§b✔ Hacha entregada. §7Ve al mundo §f" + mundoObjetivo + " §7y marca la zona de "
                + queEs + ":");
        jugador.sendMessage("§7  Clic §fizquierdo §7= esquina A   §7|   Clic §fderecho §7= esquina B");
        jugador.sendMessage("§7Con solo la esquina A ya funciona (aterrizan siempre en ese punto). "
                + "Marca tambien la B para que caigan en un punto aleatorio dentro de esa zona, "
                + "como un planeta fisico de verdad.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUso: /planeta <crear|editar|listar|teletransportar|gui|borrar|recargar|validar|area|hacha|traje|info|espacio|mundos>");
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
                return hacha(sender, args);
            case "traje":
                return traje(sender, args);
            case "info":
                return info(sender, args);
            case "espacio":
                return espacio(sender);
            case "mundos":
                return mundos(sender);
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

        if (Bukkit.getWorld(mundoPlaneta) == null && !WorldUtil.listarMundosDisponibles().contains(mundoPlaneta)) {
            sender.sendMessage("§e⚠ No encuentro ningun mundo llamado '" + mundoPlaneta + "' en la carpeta del "
                    + "servidor. Revisa §f/planeta mundos §epara ver los disponibles. Se creara igualmente, "
                    + "pero el mundo debera existir antes de usar el planeta.");
        }

        Location aqui = jugador.getLocation();
        PlanetData datos = new PlanetData(id);
        datos.setWorldName(mundoPlaneta);
        datos.setEsferaEspacio(aqui.getX(), aqui.getY(), aqui.getZ(), radio);
        datos.setEntradaAtmosfericaY(configManager.getAlturaEntradaPorDefecto());
        datos.setAlturaDespegueY(configManager.getAlturaDespeguePorDefecto());

        // La entrada y la salida YA NO llevan un valor por defecto: es
        // obligatorio marcarlas con el hacha antes de que el planeta admita
        // aterrizajes o despegues (ver entradaLista/salidaLista y TeleportManager).
        configManager.registrarPlaneta(datos);
        configManager.guardarTodo();

        sender.sendMessage("§a✔ Planeta '" + id + "' creado. Centro de la esfera: tu posicion actual.");
        sender.sendMessage("§7Siguientes pasos §c(obligatorios)§7:");
        sender.sendMessage("§7 1. Ve al mundo del planeta y marca la entrada: §f/planeta hacha entrada " + id);
        sender.sendMessage("§7    §8(o, si el schematic ocupa todo el mundo: §f/planeta editar " + id
                + " entradaworldborder §8+ pon un §f/worldborder §8razonable alli)");
        sender.sendMessage("§7 2. Vuelve al espacio, fuera de la esfera, y marca la salida: §f/planeta hacha salida " + id);
        sender.sendMessage("§7 3. Ajusta el entorno con §f/planeta gui " + id);
        sender.sendMessage("§7El planeta no permitira viajar hasta que la entrada y la salida esten listas.");
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

                case "entradaworldborder":
                    datos.setEntradaUsaWorldBorder(!datos.isEntradaUsaWorldBorder());
                    sender.sendMessage(datos.isEntradaUsaWorldBorder()
                            ? "§a✔ La entrada ahora es aleatoria dentro del WorldBorder de '" + datos.getWorldName()
                                    + "'. §7Asegurate de tener puesto un §f/worldborder §7razonable en ese mundo."
                            : "§7La entrada volvio a usar la zona marcada con el hacha.");
                    break;

                case "deteccioncaja":
                    datos.setDeteccionUsaCaja(!datos.isDeteccionUsaCaja());
                    if (datos.isDeteccionUsaCaja() && !datos.isDeteccionCajaLista()) {
                        sender.sendMessage("§e⚠ Activado, pero todavia no marcaste la caja: §f/planeta hacha deteccion "
                                + id + "§e. Mientras tanto se sigue usando la esfera normal.");
                    } else {
                        sender.sendMessage(datos.isDeteccionUsaCaja()
                                ? "§a✔ La deteccion ahora usa la caja marcada con el hacha (entras por cualquier lado)."
                                : "§7La deteccion volvio a ser la esfera normal (radio " + (int) datos.getRadio() + ").");
                    }
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
    // ---------------------------------------------------------------
    //  /planeta borrar <id> [confirmar]
    //  Borra TODO lo relacionado con el planeta: su configuracion, su
    //  seleccion/modo de hacha activo si alguien lo estaba marcando, y el
    //  MUNDO de Minecraft en si (la carpeta del mundo, del disco del
    //  servidor) — salvo que sea el mundo espacio o lo siga usando otro
    //  planeta, para no borrar algo por accidente.
    // ---------------------------------------------------------------
    private boolean borrar(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUso: /planeta borrar <id> confirmar");
            return true;
        }
        String id = args[1].toLowerCase(Locale.ROOT);
        PlanetData planeta = configManager.getPlaneta(id).orElse(null);
        if (planeta == null) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + id + "'.");
            return true;
        }

        boolean confirmado = args.length >= 3 && args[2].equalsIgnoreCase("confirmar");
        if (!confirmado) {
            sender.sendMessage("§c§l⚠ Esto borra TODO de '" + id + "' y no se puede deshacer:");
            sender.sendMessage("§7 - Su configuracion completa (entorno, entrada, salida, traje...)");
            sender.sendMessage("§7 - El §cMUNDO '" + planeta.getWorldName() + "' entero§7, del disco del servidor");
            sender.sendMessage("§7   §8(el schematic que hayas pegado ahi tambien se pierde)");
            sender.sendMessage("§eSi estas seguro, repite el comando asi:");
            sender.sendMessage("§f/planeta borrar " + id + " confirmar");
            return true;
        }

        // 1) Si alguien estaba marcando la entrada/salida de este planeta con
        //    el hacha en este mismo momento, se le cancela ese modo.
        for (Player jugador : Bukkit.getOnlinePlayers()) {
            var modo = plugin.getSelectionManager().getModoPunto(jugador);
            if (modo != null && id.equalsIgnoreCase(modo.idPlaneta())) {
                plugin.getSelectionManager().limpiarModoPunto(jugador.getUniqueId());
                jugador.sendMessage("§eEl planeta '" + id + "' que estabas marcando se acaba de borrar.");
            }
        }

        // 2) No borres el mundo si es el mundo espacio o si otro planeta lo sigue usando.
        String mundoNombre = planeta.getWorldName();
        boolean esMundoEspacio = mundoNombre.equalsIgnoreCase(configManager.getMundoEspacio());
        boolean mundoCompartido = configManager.getPlanetas().values().stream()
                .anyMatch(p -> !p.getId().equalsIgnoreCase(id) && mundoNombre.equalsIgnoreCase(p.getWorldName()));

        // 3) Borra la entrada de configuracion.
        configManager.eliminarPlaneta(id);
        configManager.guardarTodo();
        sender.sendMessage("§aPlaneta '" + id + "' borrado de la configuracion.");

        // 4) Borra el mundo del disco (con sus jugadores movidos a salvo antes).
        if (esMundoEspacio) {
            sender.sendMessage("§e⚠ '" + mundoNombre + "' es el mundo ESPACIO: no se borra, solo se quito el planeta.");
        } else if (mundoCompartido) {
            sender.sendMessage("§e⚠ Otro planeta sigue usando el mundo '" + mundoNombre + "': no se borra.");
        } else {
            borrarMundoDelDisco(sender, mundoNombre);
        }

        return true;
    }

    /** Descarga (si esta cargado) y borra por completo la carpeta de un mundo del disco. */
    private void borrarMundoDelDisco(CommandSender sender, String nombreMundo) {
        World mundo = Bukkit.getWorld(nombreMundo);
        if (mundo != null) {
            // Saca a cualquiera que siga dentro antes de tocar el mundo.
            World destino = Bukkit.getWorld(configManager.getMundoEspacio());
            for (Player jugador : new ArrayList<>(mundo.getPlayers())) {
                if (destino != null) {
                    jugador.teleport(destino.getSpawnLocation());
                }
                jugador.sendMessage("§eEl planeta en el que estabas se acaba de borrar. Te hemos movido al espacio.");
            }
            if (!Bukkit.unloadWorld(mundo, false)) {
                sender.sendMessage("§cNo se pudo descargar el mundo '" + nombreMundo + "' (puede seguir en uso "
                        + "por otro plugin). Borra su carpeta a mano si hace falta: " + mundo.getWorldFolder().getPath());
                return;
            }
        }

        File carpeta = new File(Bukkit.getWorldContainer(), nombreMundo);
        if (!carpeta.isDirectory()) {
            sender.sendMessage("§7No encontre la carpeta del mundo '" + nombreMundo + "' en disco (puede que ya no existiera).");
            return;
        }
        if (borrarCarpetaRecursivo(carpeta)) {
            sender.sendMessage("§aMundo '" + nombreMundo + "' borrado del disco por completo.");
        } else {
            sender.sendMessage("§cNo se pudo borrar del todo la carpeta '" + carpeta.getPath()
                    + "'. Revisa permisos o borra lo que quede a mano.");
        }
    }

    private boolean borrarCarpetaRecursivo(File carpeta) {
        File[] hijos = carpeta.listFiles();
        if (hijos != null) {
            for (File hijo : hijos) {
                if (hijo.isDirectory()) {
                    borrarCarpetaRecursivo(hijo);
                } else {
                    hijo.delete();
                }
            }
        }
        return carpeta.delete();
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
    //  /planeta hacha                       -> hacha normal (pos1/pos2, area)
    //  /planeta hacha entrada <id>           -> modo: marcar punto de entrada
    //  /planeta hacha salida <id>            -> modo: marcar punto de salida
    //  /planeta hacha sol                    -> modo: marcar el sol del espacio
    // ---------------------------------------------------------------
    private boolean hacha(CommandSender sender, String[] args) {
        if (!(sender instanceof Player jugador)) {
            sender.sendMessage("§cSolo un jugador puede recibir el hacha.");
            return true;
        }

        if (args.length == 1) {
            jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
            plugin.getSelectionManager().limpiarModoPunto(jugador.getUniqueId());
            jugador.sendMessage("§b✔ Hacha de seleccion entregada. §7Clic izquierdo = pos 1, clic derecho = pos 2.");
            return true;
        }

        String modo = args[1].toLowerCase(Locale.ROOT);

        if (modo.equals("sol")) {
            jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
            plugin.getSelectionManager().activarModoPunto(jugador, com.planettravel.selection.SelectionManager.TipoPunto.SOL, null);
            jugador.sendMessage("§6☀ Hacha entregada. §7Ve al mundo espacio (§f" + configManager.getMundoEspacio()
                    + "§7) y haz clic donde quieres colocar el sol.");
            return true;
        }

        if (!modo.equals("entrada") && !modo.equals("salida") && !modo.equals("deteccion")) {
            sender.sendMessage("§cUso: /planeta hacha [entrada|salida|deteccion <id> | sol]");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage("§cUso: /planeta hacha " + modo + " <id>");
            return true;
        }
        PlanetData datos = configManager.getPlaneta(args[2]).orElse(null);
        if (datos == null) {
            sender.sendMessage("§cNo existe ningun planeta con el id '" + args[2] + "'.");
            return true;
        }
        if (modo.equals("deteccion")) {
            jugador.getInventory().addItem(plugin.getSelectionManager().crearHacha());
            plugin.getSelectionManager().activarModoPunto(jugador, TipoPunto.DETECCION, datos.getId());
            jugador.sendMessage("§b✔ Hacha entregada. §7Ve al espacio (§f" + configManager.getMundoEspacio()
                    + "§7) y marca la caja de deteccion:");
            jugador.sendMessage("§7  Clic §fizquierdo §7= esquina A   §7|   Clic §fderecho §7= esquina B");
            jugador.sendMessage("§7Entraras por CUALQUIER lado de esa caja (arriba, abajo, lados). "
                    + "No olvides activarla con: §f/planeta editar " + datos.getId() + " deteccioncaja");
            return true;
        }
        entregarHachaEnModo(jugador, modo.equals("entrada") ? TipoPunto.ENTRADA : TipoPunto.SALIDA, datos);
        return true;
    }

    // ---------------------------------------------------------------
    //  /planeta mundos  -> lista todos los mundos que hay en la carpeta
    //  del servidor (cargados o no), para elegir uno al crear un planeta.
    // ---------------------------------------------------------------
    private boolean mundos(CommandSender sender) {
        List<String> mundos = WorldUtil.listarMundosDisponibles();
        sender.sendMessage("§8§m                                        ");
        sender.sendMessage("§b§lMundos disponibles §7(" + mundos.size() + ")");
        for (String nombre : mundos) {
            boolean cargado = Bukkit.getWorld(nombre) != null;
            boolean enUso = configManager.getPlanetaPorMundo(nombre) != null
                    || nombre.equals(configManager.getMundoEspacio());
            sender.sendMessage((cargado ? "§a ● " : "§7 ○ ") + "§f" + nombre
                    + (cargado ? " §8(cargado)" : " §8(en disco, sin cargar)")
                    + (enUso ? " §e- ya en uso" : ""));
        }
        sender.sendMessage("§7Usa uno de estos nombres en: §f/planeta crear <id> <mundo> <radio>");
        sender.sendMessage("§8§m                                        ");
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

        if (args.length == 3 && args[0].equalsIgnoreCase("borrar")) {
            return filtrar(Arrays.asList("confirmar"), args[2]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("area")) {
            return filtrar(Arrays.asList("we"), args[2]);
        }

        if (args[0].equalsIgnoreCase("hacha")) {
            if (args.length == 2) {
                return filtrar(Arrays.asList("entrada", "salida", "deteccion", "sol"), args[1]);
            }
            if (args.length == 3 && (args[1].equalsIgnoreCase("entrada") || args[1].equalsIgnoreCase("salida")
                    || args[1].equalsIgnoreCase("deteccion"))) {
                return filtrar(new ArrayList<>(configManager.getPlanetas().keySet()), args[2]);
            }
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("crear")) {
            return filtrar(WorldUtil.listarMundosDisponibles(), args[2]);
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("editar") && args[2].equalsIgnoreCase("mundo")) {
            return filtrar(WorldUtil.listarMundosDisponibles(), args[3]);
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
