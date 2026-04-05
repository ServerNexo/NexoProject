# AGENTS.md - Nexo Network Development Guide

## Project Overview
Nexo Network is a modular Minecraft 1.21.1 plugin system using Paper API, Maven, and Java 21.

## Build Commands

### Full Build
```bash
mvn clean install
```
Builds all modules. JARs are output to `!Compilados_Nexo/` directory.

### Single Module Build
```bash
mvn clean install -pl NexoCore -am
```
Builds NexoCore module and its dependencies.

### Skip Tests
```bash
mvn clean install -DskipTests
```

### Single Test (no tests exist currently)
```bash
mvn test -Dtest=ClassName -pl module-name
```

### Clean and Compile
```bash
mvn clean compile
```

### Package Single Module
```bash
mvn package -pl NexoPvP -DskipTests
```

## Module Structure
```
NexoCore/         - Database, API, DI (Guice), caching (Caffeine)
NexoItems/        - Custom items, backpacks
NexoPvP/          - Combat system, passive abilities
NexoColecciones/   - Farming progression, slayers
NexoMechanics/     - Skill trees, minigames
NexoMinions/       - Automation system
NexoClans/         - Clan management
NexoEconomy/       - Economy, bazaar, trading
NexoProtections/   - Claim system
NexoWar/           - War system
NexoFactories/     - Factory mechanics
NexoDungeons/      - Dungeon instances, matchmaking
```

## Code Style Guidelines

### Package Naming
- Format: `me.nexo.{module}.{submodule}.{class_type}`
- Examples: `me.nexo.pvp.pvp`, `me.nexo.clans.core`, `me.nexo.economy.bazar`

### Class Naming Conventions
- **Main plugin class**: `{ModuleName}.java` (e.g., `NexoPvP.java`, `NexoCore.java`)
- **Managers**: `{Feature}Manager.java` (e.g., `ClanManager.java`, `EconomyManager.java`)
- **Listeners**: `{Feature}Listener.java` or `{Feature}Listener.java`
- **Commands**: `Comando{Feature}.java` (Spanish naming convention)
- **Menus**: `{Feature}Menu.java` or `{Feature}Menu.java`
- **Config**: `ConfigManager.java`, `{Feature}Config.java`
- **Data classes**: `{Feature}Data.java`, `{Feature}Type.java`, `{Feature}Tier.java`

### Method Naming
- Use camelCase
- Async methods: suffix with `Async` (e.g., `setClanHomeAsync`, `getMiembrosAsync`)
- Getters: `getX()`, `isX()` for booleans
- Spanish naming acceptable for business logic methods

### Dependency Injection (Guice)
- Use `@Inject` for constructor injection
- Use `@Singleton` for single-instance services
- Register services via Module classes in `di/` package

```java
@Singleton
public class ClanManager {
    private final NexoClans plugin;
    private final NexoCore core;

    @Inject
    public ClanManager(NexoClans plugin) {
        this.plugin = plugin;
        this.core = NexoCore.getPlugin(NexoCore.class);
    }
}
```

### Listeners
```java
public class PvPListener implements Listener {
    private final PvPManager manager;

    public PvPListener(PvPManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDañoJugadores(EntityDamageByEntityEvent event) {
        // implementation
    }
}
```

### Async Operations
Always use Bukkit scheduler for async work, never block the main thread.

```java
Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
    // async work
    Bukkit.getScheduler().runTask(plugin, () -> {
        // callback on main thread
    });
});
```

### Database Operations
- Use try-with-resources for connections, statements, result sets
- Use PreparedStatement to prevent SQL injection
- Keep DB operations async when possible

```java
try (Connection conn = core.getDatabaseManager().getConnection();
     PreparedStatement ps = conn.prepareStatement(sql)) {
    ps.setString(1, id);
    ResultSet rs = ps.executeQuery();
    // process results
} catch (Exception e) {
    plugin.getLogger().severe("Error: " + e.getMessage());
}
```

### Caching
Use Caffeine cache with expiration policies:
```java
private final Cache<UUID, NexoClan> clanCache = Caffeine.newBuilder()
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();
```

### Logging
- Use `getLogger().info()` for startup messages
- Use `getLogger().severe()` for errors
- Include context in error messages

### Null Safety
- Prefer `Optional` for methods that may return null
- Use `Optional.ofNullable()` and `ifPresent()`
- Use `getIfPresent()` for cache lookups

### Configuration
- Use Configurate YAML loader for type-safe config
- Use legacy `YamlConfiguration` for simple configs when needed
- Path format: `{category}.{subcategory}.{action}` (e.g., `comandos.clan.exito.home-establecido`)

### Comments
- Use Spanish comments throughout the codebase
- Javadoc for public APIs and complex methods
- Inline comments for non-obvious logic

### Imports Organization
1. Java standard library
2. Third-party libraries (Paper, Guice, etc.)
3. Internal modules (NexoCore, etc.)
4. Within same module (no empty line needed)

### Error Handling
- Catch specific exceptions when possible
- Log errors with context
- Don't swallow exceptions silently unless intentional

### Type-Safe Records (if applicable)
Use Java records for simple data carriers:
```java
public record ClanMember(UUID uuid, String name, String clanRole) {}
```

### Service Registration
For cross-module communication, register via NexoAPI:
```java
NexoAPI.getServices().register(WaveManager.class, this.waveManager);
NexoAPI.getServices().get(ClaimManager.class).ifPresent(manager -> {
    // use manager
});
```

## Testing
- No test framework currently configured
- Unit tests should go in `src/test/java/`
- Use `mvn test -Dtest=ClassName -pl module-name` to run specific tests
