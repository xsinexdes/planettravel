# PlanetTravel

Plugin para Paper 1.21.1 que gestiona el viaje entre un mundo **"espacio"** y varios
mundos-planeta, integrandose con la API publica del plugin **CoreNaves** para mover
naves completas entre mundos.

## Compilar

```bash
mvn clean package
```

El `.jar` queda en `target/PlanetTravel-1.0.0.jar`. El repositorio incluye tambien
un workflow de GitHub Actions (`.github/workflows/build.yml`) que compila el jar
automaticamente en cada push y lo deja descargable como artefacto de la Action.

## Estado de la integracion con CoreNaves

**CoreNaves todavia no existe.** Para no bloquear el desarrollo de PlanetTravel,
el plugin define en `com.planettravel.api` el contrato (`CoreNavesAPI`, `Ship`)
que CoreNaves debera implementar el dia que exista, y lo busca en el
`ServicesManager` de Bukkit al arrancar (`CoreNavesHook`).

Mientras tanto, con `modo-sin-corenaves: true` en `config.yml` (valor por
defecto), PlanetTravel usa una implementacion de respaldo
(`CoreNavesAPIFallback`) que mueve solo al jugador, para poder probar toda la
deteccion de aterrizaje/despegue y los comandos sin esperar al otro plugin.

### Que debera hacer CoreNaves cuando exista

En su `onEnable()`, CoreNaves debe registrar su implementacion de `CoreNavesAPI`
(la interfaz esta pensada para copiarla o para que PlanetTravel la exponga como
dependencia/API compartida):

```java
Bukkit.getServicesManager().register(
    CoreNavesAPI.class, miImplementacion, this, ServicePriority.Normal);
```

En cuanto eso ocurra (y `CoreNaves` este listado en el `softdepend` de
PlanetTravel, ya lo esta), `CoreNavesHook` detectara automaticamente la
implementacion real y dejara de usar el modo de respaldo — sin tocar nada mas
del codigo.

## Configuracion (`config.yml`)

Cada planeta define, dentro de `planetas.<id>`:

- `mundo`: nombre del mundo-planeta.
- `espacio.x/y/z/radio`: centro y radio de la esfera en el mundo "espacio".
- `entrada-atmosferica-y`: altura a la que aparece la nave al aterrizar (por defecto 1400).
- `spawn-planeta.x/y/z/yaw/pitch`: punto exacto de aterrizaje en el mundo del planeta.
- `altura-despegue-y`: altura a partir de la cual, ascendiendo, se considera despegue (por defecto 1500).
- `salida-espacio.x/y/z/yaw/pitch`: donde reaparece la nave en el mundo "espacio" al despegar.

Opciones globales:

- `mundo-espacio`: nombre del mundo "espacio".
- `modo-sin-corenaves`: true/false (ver seccion anterior).
- `intervalo-comprobacion-ticks`: cada cuantos ticks se comprueba la posicion de un jugador que pilota (throttling).
- `cooldown-teletransporte-segundos`: cooldown anti-bucle tras cada teletransporte.

## Comandos

Permiso: `planettravel.admin` (por defecto: op).

```
/planeta crear <id> <mundoPlaneta> <radio>
/planeta editar <id> <campo> <valores...>
/planeta listar
/planeta teletransportar <id> [jugador]
```

Campos de `/planeta editar`: `mundo`, `centro`, `centroaqui`, `radio`, `entrada`,
`despegue`, `spawnplaneta`, `spawnaqui`, `salidaespacio`, `salidaaqui`.
Los campos "*aqui" usan tu posicion actual en el juego, para no tener que
calcular coordenadas a mano.

### Flujo tipico para configurar un planeta nuevo

1. Vuela hasta donde quieras que este la esfera de Marte en el mundo "espacio".
2. `/planeta crear marte planeta_marte 150`
3. Viaja (o teletransportate) al mundo `planeta_marte`, colocate donde quieras
   que aterricen las naves y ejecuta `/planeta editar marte spawnaqui`.
4. Vuelve al mundo "espacio", ponte justo donde quieras que reaparezcan las
   naves al despegar de Marte, y ejecuta `/planeta editar marte salidaaqui`.
5. Ajusta alturas si quieres con `/planeta editar marte entrada <y>` y
   `/planeta editar marte despegue <y>`.

## Estructura del codigo

```
com.planettravel
 ├─ PlanetTravel.java          (clase principal / arranque)
 ├─ config/                    (PlanetData, PlanetConfigManager -> lee/escribe config.yml)
 ├─ api/                       (contrato CoreNavesAPI/Ship + hook + respaldo de pruebas)
 ├─ teleport/                  (TeleportManager -> ejecuta los TP, cooldowns, efectos)
 ├─ listeners/                 (ShipMovementListener -> deteccion en tiempo real)
 ├─ commands/                  (PlanetCommand -> /planeta)
 └─ util/                      (WorldUtil -> carga de mundos-planeta)
```


---

# Novedades de la version 1.1.0

## Alturas ajustadas a mundos normales

Las alturas por defecto son ahora **Y=310 (entrada)** y **Y=315 (despegue)**,
que caben en cualquier mundo de Minecraft estandar (techo Y=320). Si tus
mundos-planeta estan generados con altura extendida, puedes subirlas.

El plugin ahora **valida las alturas al arrancar** contra el techo real de cada
mundo y avisa en consola si configuraste un valor imposible, en vez de dejar
que las naves aparezcan en el vacio.

## Radio de deteccion vs radio visual

Son dos cosas distintas y es importante no confundirlas:

- `radio` — la zona **invisible** de deteccion. Al entrar aqui, se aterriza.
- `radio-visual` — el radio de la esfera de **bloques** que hiciste con WorldEdit.

El de deteccion debe ser **mayor** que el visual, o la nave chocara contra los
bloques antes de que salte el teletransporte. Ejemplo tipico: esfera de
WorldEdit de 150 → `radio-visual: 150`, `radio: 180`.

`/planeta validar` te avisa si los tienes al reves.

## Validaciones anti-bucle

El plugin detecta y avisa de las configuraciones que causarian bucles de
teletransporte, que el cooldown por si solo no arregla:

- La salida al espacio de un planeta cae dentro de su **propia** esfera.
- La salida de un planeta cae dentro de la esfera de **otro** planeta.
- Dos esferas se **solapan** (la deteccion seria impredecible).
- La altura de despegue es menor o igual que la de entrada.

Se muestran al arrancar, tras cada edicion, y a demanda con `/planeta validar`.

## Feedback visual

- **BossBar de altitud**: al pilotar en un planeta, una barra muestra lo cerca
  que estas de la altura de escape, cambiando de azul a amarillo a verde con un
  "ALTITUD DE ESCAPE ALCANZADA" al llegar.
- **Actionbar de proximidad**: en el espacio, muestra el planeta mas cercano y
  cuantos bloques faltan para el borde atmosferico.
- **Borde visible de la esfera**: un anillo de particulas cian marca donde
  empieza la zona de aterrizaje cuando te acercas.
- **Entrada atmosferica prolongada**: en vez de un flash, una estela de fuego,
  humo y chispas sigue a la nave durante ~4,5 segundos mientras cae, con rugido
  de friccion y sacudida de camara que se van apagando.

## Sistema de entorno por planeta

Cada planeta tiene su propio entorno configurable:

| Ajuste | Que hace |
|---|---|
| **Casco espacial** | Sin el casco configurado, asfixia progresiva (danio + nausea + ceguera). Opcionalmente exige traje completo. |
| **Agua toxica** | El agua hace danio al contacto, con veneno residual opcional. |
| **Gravedad** | De 0.1x a 3.0x. Baja = saltos altos y caida lenta. Alta = lentitud y caidas pesadas. |
| **Temperatura** | Sistema completo (ver abajo). |
| **Efectos permanentes** | Cualquier combinacion de 18 efectos con nivel 1-5. |
| **Clima y hora** | Fijar lluvia/despejado y detener el ciclo dia-noche. |

### Sistema de temperatura

La temperatura que siente el jugador se calcula como:

```
ambiente = base
         + variacion dia/noche (curva suave segun la hora del mundo)
         + variacion por altura (por cada 100 bloques)
         + modificadores locales (lava y fuego calientan, agua y hielo enfrian)
         + lluvia si estas a la intemperie
```

Detalles que hacen que se sienta bien:

- La temperatura del jugador **no salta**: se interpola gradualmente hacia la
  ambiente, asi que refugiarse en una cueva da alivio progresivo y salir al
  exterior no te mata en el primer tick.
- **La armadura protege**: el cuero abriga contra el frio pero da calor; el
  netherite resiste el fuego; el casco espacial aisla de ambos extremos.
  La ropa te acerca a la zona segura, nunca te pone a 20°C de golpe.
- **Estar bajo tierra** estabiliza la temperatura.
- Pasarse del umbral da danio **escalado**: cuanto mas extremo, mas duele.
  Calor = danio + hambre + humo. Frio = danio + lentitud + fatiga + copos.

## GUIs

### `/planetas` — Mapa estelar (todos los jugadores)

Muestra cada planeta con su icono, distancia desde tu posicion, coordenadas y
un **resumen completo de sus peligros**: si hace falta casco, si el agua es
toxica, la gravedad, los rangos de temperatura seguros y los efectos
permanentes. Asi el jugador sabe con que equipo ir antes de despegar.

### `/planeta gui <id>` — Editor de planeta (admins)

Editor visual completo del entorno. Convencion de clics en todo el editor:

- **Clic izquierdo** = activar / subir valor
- **Clic derecho** = desactivar / bajar valor
- **Shift** = paso grande (x5)

Incluye un selector con **18 efectos curados** (los que tienen sentido
tematicamente en un planeta), cada uno con una nota de en que tipo de mundo
encaja: lentitud para terreno denso, ceguera para tormentas de polvo,
marchitamiento para radiacion, levitacion para anomalias gravitatorias...

Todo se guarda al YAML automaticamente en cada cambio.

## Comandos nuevos

```
/planetas                        Mapa estelar (permiso: planettravel.usar, por defecto todos)
/planeta gui [id]                Mapa estelar o editor de un planeta
/planeta borrar <id>             Quita el planeta de la config (no toca el mundo)
/planeta recargar                Recarga config.yml
/planeta validar                 Revisa problemas de configuracion
```

Campos nuevos en `/planeta editar`: `radiovisual`, `nombre`, `descripcion`, `icono`.

## Mejoras internas

- **Deteccion por tarea programada** en vez de solo `PlayerMoveEvent`: si
  CoreNaves mueve la nave con el piloto quieto, el evento no se disparaba y el
  aterrizaje no se detectaba. Ademas es mas barato.
- **Precarga asincrona del chunk destino** antes de teletransportar, con vuelta
  explicita al hilo principal para el TP. Evita el tiron del servidor.
- **Limpieza de memoria** al desconectar: cooldowns, temperaturas y bossbars.
- Getters directos de coordenadas en `PlanetData` (antes el guardado construia
  `Location` con mundo nulo solo para leer numeros).
