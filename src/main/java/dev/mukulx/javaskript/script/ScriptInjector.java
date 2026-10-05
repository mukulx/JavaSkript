package dev.mukulx.javaskript.script;

import dev.mukulx.javaskript.JavaSkriptPlugin;
import dev.mukulx.javaskript.api.*;
import dev.mukulx.javaskript.api.chat.ChatHelper;
import dev.mukulx.javaskript.api.command.CommandHelper;
import dev.mukulx.javaskript.api.event.EventHelper;
import dev.mukulx.javaskript.api.item.ItemHelper;
import dev.mukulx.javaskript.api.player.PlayerHelper;
import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Level;

/** Hands the script its API helpers by field type or name. */
final class ScriptInjector {

  private ScriptInjector() {}

  static void inject(JavaSkriptPlugin plugin, ScriptInstance owner, Object instance) {
    Class<?> scriptClass = owner.getScriptClass();
    File scriptFile = owner.getScriptFile();
    try {
      // Collect and inject into all fields across the entire class hierarchy
      Class<?> current = scriptClass;
      while (current != null && current != Object.class) {
        for (Field field : current.getDeclaredFields()) {
          try {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || java.lang.reflect.Modifier.isFinal(field.getModifiers())) {
              continue;
            }
            field.setAccessible(true);

            // Skip if already initialized with non-null value
            if (field.get(instance) != null) {
              continue;
            }

            Class<?> type = field.getType();
            String name = field.getName().toLowerCase();

            // Matched by field type only: a field name never decides what a script is given
            if (JavaSkriptPlugin.class.isAssignableFrom(type)) {
              field.set(instance, plugin);
            } else if (JavaSkriptAPI.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI());
            } else if (ScriptScheduler.class.isAssignableFrom(type)) {
              field.set(instance, owner.getScheduler());
            } else if (ScriptConfig.class.isAssignableFrom(type)) {
              field.set(instance, owner.getConfig());
            } else if (DatabaseHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getDatabase());
            } else if (PlaceholderHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getPlaceholders());
            } else if (RecipeHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.recipes());
            } else if (ActionBarHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.actionBars());
            } else if (TitleHelper.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI().getTitleHelper());
            } else if (BossBarHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.bossBars());
            } else if (SoundHelper.class.isAssignableFrom(type)) {
              field.set(instance, plugin.getAPI().getSoundHelper());
            } else if (DialogHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getDialog());
            } else if (PDCHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getPDC());
            } else if (HologramHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getHolograms());
            } else if (dev.mukulx.javaskript.api.MannequinHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getMannequins());
            } else if (dev.mukulx.javaskript.api.AdvancementHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getAdvancements());
            } else if (CommandHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getCommands());
            } else if (ItemHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getItems());
            } else if (CooldownHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getCooldowns());
            } else if (EventHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getEvents());
            } else if (PlayerHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getPlayers());
            } else if (ChatHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getChat());
            } else if (dev.mukulx.javaskript.api.economy.EconomyHelper.class.isAssignableFrom(type)
                || dev.mukulx.javaskript.api.economy.EconomyProvider.class.isAssignableFrom(type)) {
              field.set(instance, owner.getEconomy());
            } else if (dev.mukulx.javaskript.api.variable.ScriptVariables.class.isAssignableFrom(
                type)) {
              field.set(instance, owner.scriptVariables());
            } else if (dev.mukulx.javaskript.api.variable.VariableHelper.class.isAssignableFrom(
                type)) {
              field.set(instance, owner.getVariables());
            } else if (dev.mukulx.javaskript.api.http.HttpHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getHttp());
            } else if (dev.mukulx.javaskript.api.team.TeamHelper.class.isAssignableFrom(type)) {
              field.set(instance, owner.getTeams());
            } else if (dev.mukulx.javaskript.api.message.MessageManager.class.isAssignableFrom(
                type)) {
              field.set(instance, plugin.getMessageManager());
            } else {
              // Check external addons and plugins for custom registered field injectors
              Object custom = plugin.getAPI().resolveCustomInjection(owner, type, name);
              if (custom != null) {
                field.set(instance, custom);
              }
            }
          } catch (Exception e) {
            plugin.debug("Could not inject into field " + field.getName() + ": " + e.getMessage());
          }
        }
        current = current.getSuperclass();
      }
    } catch (Exception e) {
      plugin
          .getLogger()
          .log(Level.WARNING, "Error during API injection for " + scriptFile.getName(), e);
    }
  }
}
