# 🛡️ Nexo Network - Enterprise Modular System

Este es el ecosistema de plugins (microservicios) diseñado a medida para **Nexo Network** (Minecraft 1.21+).
El proyecto está construido bajo una estricta **Arquitectura Enterprise** enfocada en el máximo rendimiento, la compatibilidad Crossplay (Java/Bedrock) y la eliminación absoluta de lag por I/O (lectura de disco).

## 🏗️ Arquitectura y Estándares de Desarrollo
Cualquier código nuevo añadido a este proyecto **debe** respetar los siguientes pilares:
1. **Inyección de Dependencias (Google Guice):** No se permiten `new Manager()`. Todos los servicios, listeners y comandos deben ser inyectados vía `@Singleton` e `@Inject`.
2. **Sponge Configurate:** Prohibido usar el `YamlConfiguration` nativo de Bukkit para leer mensajes en tiempo real. Todo se mapea en Nodos Tipados (`@ConfigSerializable`) al arrancar el servidor.
3. **Cero Lag Visual (CrossplayUtils):** Los textos, lores y títulos deben usar componentes directos y el sistema Hexadecimal (`&#RRGGBB`) procesado por `LegacyComponentSerializer` para evitar bugs visuales en Bedrock.
4. **Hilos Virtuales y FAWE:** Las operaciones pesadas (generación de esquemas, lecturas SQL) deben ir en `CompletableFuture.supplyAsync` o `Thread.startVirtualThread`.

---

## 📦 Estructura de Módulos (Microservicios)

El ecosistema está dividido en submódulos Maven totalmente independientes que se conectan a través de la API de `NexoCore`.

* 🧠 **`NexoCore`**: El corazón del sistema. Gestiona la base de datos (HikariCP / Supabase), perfiles de usuario, menús globales, optimización de partículas y utilidades Crossplay.
* 🎒 **`NexoItems`**: Motor de RPG. Gestiona ítems custom, mochilas, guardarropas, reforjas, encantamientos, accesorios y artefactos.
* ⚔️ **`NexoPvP`**: Sistema de combate, clases de armadura, árbol de habilidades pasivas y penalizaciones de muerte.
* 🏰 **`NexoDungeons`**: Generador de instancias asíncronas con FAWE. Incluye motor de puzzles matemáticos, matchmaking de escuadrones, oleadas y jefes globales.
* 💰 **`NexoEconomy`**: Economía atómica (Coins, Gems, Mana). Gestiona el Bazar (órdenes de compra/venta), el Mercado Negro y el sistema de intercambios (Trades) seguros.
* 🛡️ **`NexoProtections`**: Sistema de claims mediante Piedras de Protección, gestión de límites, permisos de miembros e impuestos de mantenimiento (Upkeep).
* ⚙️ **`NexoMechanics`**: Sistema de profesiones y minijuegos inmersivos (Tala, Minería, Pesca, Agricultura, Alquimia y Combate).
* 📚 **`NexoColecciones`**: Progresión a largo plazo. Sistema de farmeo de colecciones por categorías (Tiers) y jefes *Slayer*.
* 🤖 **`NexoMinions`**: Automatización de recolección en islas/protecciones mediante entidades (Minions) mejorables.
* 🏭 **`NexoFactories`**: Construcción avanzada. Estructuras automatizadas controladas por scripts y lógica de evaluadores.
* 👥 **`NexoClans`**: Ecosistema social, gestión de miembros, rangos, daño aliado y chat privado de clan.

---

## 🛠️ Compilación y Despliegue

Este proyecto utiliza **Maven** como gestor de dependencias. Para compilar todo el ecosistema y generar los archivos ejecutables:

1. Abre una terminal en el directorio raíz del proyecto.
2. Ejecuta el comando maestro:
   ```bash
   mvn clean install