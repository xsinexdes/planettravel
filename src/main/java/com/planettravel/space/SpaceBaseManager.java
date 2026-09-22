package com.planettravel.space;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda y busca las bases espaciales. Van en su propio archivo (bases.yml)
 * para no mezclarlas con la configuracion de planetas.
 */
public class SpaceBaseManager {

    private final JavaPlugin plugin;
    private final File archivo;
    private final Map<String, SpaceBase> bases = new LinkedHashMap<>();

    public SpaceBaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.archivo = new File(plugin.getDataFolder(), "bases.yml");
    }

    public void cargar() {
        bases.clear();
        if (!archivo.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);
        ConfigurationSection seccion = yaml.getConfigurationSection("bases");
        if (seccion == null) {
            return;
        }

        for (String id : seccion.getKeys(false)) {
            ConfigurationSection s = seccion.getConfigurationSection(id);
            if (s == null) continue;

            SpaceBase b = new SpaceBase(id,
                    s.getString("nombre", id),
                    s.getString("mundo", "espacio"),
                    s.getInt("min.x"), s.getInt("min.y"), s.getInt("min.z"),
                    s.getInt("max.x"), s.getInt("max.y"), s.getInt("max.z"));

            b.setOxigeno(s.getBoolean("oxigeno", true));
            b.setTemperatura(s.getDouble("temperatura", 21.0));
            b.setGravedadArtificial(s.getBoolean("gravedad-artificial", true));

            try {
                b.setTipo(SpaceBase.Tipo.valueOf(s.getString("tipo", "ADMIN").toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                b.setTipo(SpaceBase.Tipo.ADMIN);
            }
            String dueno = s.getString("propietario", "");
            if (dueno != null && !dueno.isEmpty()) {
                try {
                    b.setPropietario(UUID.fromString(dueno));
                } catch (IllegalArgumentException ignorado) {
                    // UUID invalido: se deja sin propietario
                }
            }
            b.setRequiereFuel(s.getBoolean("requiere-fuel", false));
            b.setFuelMax(s.getDouble("fuel-max", 1000.0));
            b.setFuelActual(s.getDouble("fuel-actual", 0.0));
            b.setConsumoPorMinuto(s.getDouble("consumo-por-minuto", 1.0));

            bases.put(id.toLowerCase(Locale.ROOT), b);
        }
    }

    public void guardar() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (SpaceBase b : bases.values()) {
            String base = "bases." + b.getId();
            yaml.set(base + ".nombre", b.getNombre());
            yaml.set(base + ".mundo", b.getMundo());
            yaml.set(base + ".min.x", b.getMinX());
            yaml.set(base + ".min.y", b.getMinY());
            yaml.set(base + ".min.z", b.getMinZ());
            yaml.set(base + ".max.x", b.getMaxX());
            yaml.set(base + ".max.y", b.getMaxY());
            yaml.set(base + ".max.z", b.getMaxZ());
            yaml.set(base + ".oxigeno", b.isOxigeno());
            yaml.set(base + ".temperatura", b.getTemperatura());
            yaml.set(base + ".gravedad-artificial", b.isGravedadArtificial());

            // Reservado para el futuro (bases de jugadores con fuel)
            yaml.set(base + ".tipo", b.getTipo().name());
            yaml.set(base + ".propietario", b.getPropietario() == null ? "" : b.getPropietario().toString());
            yaml.set(base + ".requiere-fuel", b.isRequiereFuel());
            yaml.set(base + ".fuel-actual", b.getFuelActual());
            yaml.set(base + ".fuel-max", b.getFuelMax());
            yaml.set(base + ".consumo-por-minuto", b.getConsumoPorMinuto());
        }
        try {
            plugin.getDataFolder().mkdirs();
            yaml.save(archivo);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudo guardar bases.yml: " + e.getMessage());
        }
    }

    /** Base que contiene esta posicion, o null. Si hay varias solapadas, gana la primera creada. */
    public SpaceBase baseEn(Location loc) {
        for (SpaceBase b : bases.values()) {
            if (b.contiene(loc)) {
                return b;
            }
        }
        return null;
    }

    public SpaceBase get(String id) {
        return bases.get(id.toLowerCase(Locale.ROOT));
    }

    public void registrar(SpaceBase base) {
        bases.put(base.getId().toLowerCase(Locale.ROOT), base);
        guardar();
    }

    public boolean eliminar(String id) {
        boolean existia = bases.remove(id.toLowerCase(Locale.ROOT)) != null;
        if (existia) {
            guardar();
        }
        return existia;
    }

    public Collection<SpaceBase> getBases() {
        return bases.values();
    }
}
