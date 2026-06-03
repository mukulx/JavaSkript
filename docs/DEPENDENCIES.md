# Dynamic Dependencies

JavaSkript supports dynamic Maven dependencies, allowing scripts to use any library from Maven Central without manually downloading JARs.

## How It Works

When a script declares dependencies, JavaSkript automatically:
1. Downloads the library and all transitive dependencies from Maven Central
2. Adds them to the compilation classpath
3. Injects them into the script's isolated ClassLoader at runtime

## Declaring Dependencies

There are two ways to declare dependencies in your scripts:

### Method 1: Comment-Based (Recommended)

Add `// @dependency` comments at the top of your script:

```java
// @dependency com.zaxxer:HikariCP:5.1.0
// @dependency com.mysql:mysql-connector-j:9.1.0

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class MyScript implements Listener {
    // Your code here
}
```

### Method 2: Annotation-Based

Use the `@ScriptDependencies` annotation:

```java
import dev.mukulx.javaskript.script.ScriptDependencies;

@ScriptDependencies({
    "com.zaxxer:HikariCP:5.1.0",
    "com.mysql:mysql-connector-j:9.1.0"
})
public class MyScript implements Listener {
    // Your code here
}
```

## Dependency Format

Dependencies use Maven coordinate format:

```
groupId:artifactId:version
```

Examples:
- `com.zaxxer:HikariCP:5.1.0`
- `com.mysql:mysql-connector-j:9.1.0`
- `com.squareup.okhttp3:okhttp:4.12.0`
- `org.apache.commons:commons-lang3:3.14.0`

## Finding Dependencies

Search for libraries on [Maven Central](https://search.maven.org/):

1. Search for the library name
2. Click on the latest version
3. Copy the Maven coordinates (groupId:artifactId:version)

## Examples

### MySQL Database with HikariCP

```java
// @dependency com.zaxxer:HikariCP:5.1.0
// @dependency com.mysql:mysql-connector-j:9.1.0

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.mukulx.javaskript.script.FoliaSupport;
import org.bukkit.event.Listener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@FoliaSupport
public class MySQLScript implements Listener {
    private HikariDataSource dataSource;
    
    public MySQLScript() {
        // Configure HikariCP
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/minecraft");
        config.setUsername("root");
        config.setPassword("password");
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        
        dataSource = new HikariDataSource(config);
        System.out.println("[MySQLScript] Connected to MySQL database!");
    }
    
    public void onDisable() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("[MySQLScript] Database connection closed");
        }
    }
    
    public void savePlayerData(String playerName, int coins) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO players (name, coins) VALUES (?, ?) " +
                 "ON DUPLICATE KEY UPDATE coins = ?")) {
            
            stmt.setString(1, playerName);
            stmt.setInt(2, coins);
            stmt.setInt(3, coins);
            stmt.executeUpdate();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### HTTP Requests with OkHttp

```java
// @dependency com.squareup.okhttp3:okhttp:4.12.0

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HTTPScript implements CommandExecutor {
    private final OkHttpClient client = new OkHttpClient();
    
    public void onLoad() {
        JavaSkriptAPI.registerCommand("httptest", this);
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Request request = new Request.Builder()
            .url("https://api.github.com/users/mukulx")
            .build();
            
        try (Response response = client.newCall(request).execute()) {
            sender.sendMessage("Response: " + response.body().string());
        } catch (Exception e) {
            sender.sendMessage("Error: " + e.getMessage());
        }
        return true;
    }
}
```

### JSON Processing with Gson

```java
// @dependency com.google.code.gson:gson:2.11.0

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.mukulx.javaskript.api.JavaSkriptAPI;

public class JSONScript implements Listener {
    private final Gson gson = new Gson();
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        JsonObject data = new JsonObject();
        data.addProperty("player", event.getPlayer().getName());
        data.addProperty("time", System.currentTimeMillis());
        
        String json = gson.toJson(data);
        JavaSkriptAPI.getPlugin().getLogger().info("Player data: " + json);
    }
}
```

## Caching

Dependencies are cached after the first download:
- Downloaded JARs are stored in `plugins/JavaSkript/libs/`
- Subsequent script loads use the cached version
- No internet connection needed after initial download

## Transitive Dependencies

JavaSkript automatically resolves and downloads transitive dependencies (dependencies of dependencies).

For example, if you depend on:
```java
// @dependency com.zaxxer:HikariCP:5.1.0
```

JavaSkript will also download:
- `org.slf4j:slf4j-api` (logging interface)
- And all other required dependencies

## Troubleshooting

### Compilation Errors

If you get "package does not exist" errors:
1. Check the dependency coordinate is correct
2. Verify the library exists on Maven Central
3. Check the server logs for download errors

### Runtime Errors (NoClassDefFoundError)

If compilation succeeds but runtime fails:
1. The dependency may have native code (not supported)
2. Check for conflicting versions with other plugins
3. Review server logs for ClassLoader issues

### Download Failures

If dependencies fail to download:
1. Check internet connection
2. Verify Maven Central is accessible
3. Try a different version of the library
4. Check server logs for detailed error messages

## Limitations

1. **No Native Libraries**: Cannot use libraries with native code (JNI)
2. **No Snapshots**: Only release versions from Maven Central
3. **No Custom Repositories**: Only Maven Central is supported
4. **Size Limits**: Very large libraries may cause memory issues

## Best Practices

1. **Use Specific Versions**: Always specify exact versions, not ranges
2. **Minimize Dependencies**: Only add what you actually need
3. **Test Thoroughly**: Test scripts with dependencies before production
4. **Document Dependencies**: Comment why each dependency is needed
5. **Check Licenses**: Ensure library licenses are compatible with your use case

## Performance

- First load: Downloads dependencies (may take a few seconds)
- Subsequent loads: Uses cached JARs (fast)
- Memory: Each script gets isolated ClassLoader with dependencies
- No impact on other scripts or plugins

## Security

- Dependencies are downloaded over HTTPS from Maven Central
- No code execution during download
- Dependencies run in isolated ClassLoaders
- Same security model as regular Bukkit plugins
