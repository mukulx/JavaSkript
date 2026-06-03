import dev.mukulx.javaskript.api.ScriptScheduler;
import dev.mukulx.javaskript.script.FoliaSupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.server.PluginEnableEvent;

@FoliaSupport
public class SchedulerExample implements Listener {

  private ScriptScheduler scheduler;
  private boolean tasksStarted = false;

  @EventHandler
  public void onPluginEnable(PluginEnableEvent event) {
    if (!event.getPlugin().getName().equals("JavaSkript")) return;
    if (tasksStarted) return;

    tasksStarted = true;

    scheduler.everyMinute(
        () -> {
          Bukkit.broadcast(
              Component.text("This message appears every minute").color(NamedTextColor.YELLOW));
        });

    scheduler.everyHour(
        () -> {
          System.out.println("[SchedulerExample] Hourly task executed");
        });
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    if (event == null || event.getPlayer() == null) return;

    var player = event.getPlayer();

    scheduler.runLater(
        () -> {
          player.sendMessage(
              Component.text("Welcome! This message was delayed by 5 seconds")
                  .color(NamedTextColor.GREEN));
        },
        100L);

    final int[] count = {10};
    scheduler.runTimer(
        () -> {
          if (count[0] > 0) {
            player.sendActionBar(
                Component.text("Countdown: " + count[0]).color(NamedTextColor.GOLD));
            count[0]--;
          }
        },
        0L,
        20L);

    scheduler.runAsync(
        () -> {
          String result = performHeavyCalculation();

          scheduler.run(
              () -> {
                player.sendMessage(
                    Component.text("Calculation result: " + result).color(NamedTextColor.AQUA));
              });
        });
  }

  private String performHeavyCalculation() {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return "42";
  }
}
