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

            // 1. Match by Type
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
            }
            // 2. Match by Name / Alias
            else if (name.equals("plugin") || name.equals("javaskript")) {
              field.set(instance, plugin);
            } else if (name.equals("api")) {
              field.set(instance, plugin.getAPI());
            } else if (name.equals("scheduler") || name.equals("tasks")) {
              field.set(instance, owner.getScheduler());
            } else if (name.equals("config") || name.equals("cfg")) {
              field.set(instance, owner.getConfig());
            } else if (name.equals("database") || name.equals("db")) {
              field.set(instance, owner.getDatabase());
            } else if (name.equals("placeholders") || name.equals("papi")) {
              field.set(instance, owner.getPlaceholders());
            } else if (name.equals("recipes")) {
              field.set(instance, owner.recipes());
            } else if (name.equals("actionbar")
                || name.equals("actionbars")
                || name.equals("actionbarhelper")) {
              field.set(instance, owner.actionBars());
            } else if (name.equals("title")
                || name.equals("titles")
                || name.equals("titlehelper")) {
              field.set(instance, plugin.getAPI().getTitleHelper());
            } else if (name.equals("bossbar")
                || name.equals("bossbars")
                || name.equals("bossbarhelper")) {
              field.set(instance, owner.bossBars());
            } else if (name.equals("sound")
                || name.equals("sounds")
                || name.equals("soundhelper")) {
              field.set(instance, plugin.getAPI().getSoundHelper());
            } else if (name.equals("dialog")
                || name.equals("dialogs")
                || name.equals("dialoghelper")) {
              field.set(instance, owner.getDialog());
            } else if (name.equals("pdc")
                || name.equals("pdchelper")
                || name.equals("persistentdata")
                || name.equals("nbt")) {
              field.set(instance, owner.getPDC());
            } else if (name.equals("hologram")
                || name.equals("holograms")
                || name.equals("holo")
                || name.equals("displays")) {
              field.set(instance, owner.getHolograms());
            } else if (name.equals("mannequin")
                || name.equals("mannequins")
                || name.equals("mannequinhelper")
                || name.equals("npc")
                || name.equals("npcs")
                || name.equals("statue")
                || name.equals("statues")) {
              field.set(instance, owner.getMannequins());
            } else if (name.equals("advancement")
                || name.equals("advancements")
                || name.equals("advancementhelper")
                || name.equals("toasts")
                || name.equals("toast")) {
              field.set(instance, owner.getAdvancements());
            } else if (name.equals("commands")
                || name.equals("commandhelper")
                || name.equals("commandapi")
                || name.equals("cmd")) {
              field.set(instance, owner.getCommands());
            } else if (name.equals("items")
                || name.equals("itemhelper")
                || name.equals("itembuilder")) {
              field.set(instance, owner.getItems());
            } else if (name.equals("cooldowns")
                || name.equals("cooldown")
                || name.equals("cooldownhelper")) {
              field.set(instance, owner.getCooldowns());
            } else if (name.equals("events")
                || name.equals("eventhelper")
                || name.equals("eventapi")) {
              field.set(instance, owner.getEvents());
            } else if (name.equals("players")
                || name.equals("playerhelper")
                || name.equals("playerutil")) {
              field.set(instance, owner.getPlayers());
            } else if (name.equals("chat") || name.equals("chathelper") || name.equals("chatapi")) {
              field.set(instance, owner.getChat());
            } else if (name.equals("economy")
                || name.equals("eco")
                || name.equals("economyhelper")
                || name.equals("vault")) {
              field.set(instance, owner.getEconomy());
            } else if (name.equals("scriptvariables") || name.equals("localvariables")) {
              field.set(instance, owner.scriptVariables());
            } else if (name.equals("variables")
                || name.equals("vars")
                || name.equals("variablehelper")
                || name.equals("shared")
                || name.equals("state")) {
              field.set(instance, owner.getVariables());
            } else if (name.equals("http") || name.equals("web") || name.equals("httphelper")) {
              field.set(instance, owner.getHttp());
            } else if (name.equals("teams")
                || name.equals("team")
                || name.equals("teamhelper")
                || name.equals("clan")
                || name.equals("party")) {
              field.set(instance, owner.getTeams());
            } else if (name.equals("messages")
                || name.equals("messagemanager")
                || name.equals("messagehelper")) {
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
