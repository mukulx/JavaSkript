package dev.mukulx.javaskript.api.http;

import java.time.Instant;
import java.util.*;

/** Represents a Discord rich embed message. */
public class DiscordEmbed {

  public static class Field {
    private final String name;
    private final String value;
    private final boolean inline;

    public Field(String name, String value, boolean inline) {
      this.name = name;
      this.value = value;
      this.inline = inline;
    }

    public Map<String, Object> toMap() {
      Map<String, Object> map = new LinkedHashMap<>();
      map.put("name", name != null ? name : "");
      map.put("value", value != null ? value : "");
      map.put("inline", inline);
      return map;
    }
  }

  private String title;
  private String description;
  private String url;
  private Integer color;
  private String timestamp;
  private Map<String, String> footer;
  private Map<String, String> image;
  private Map<String, String> thumbnail;
  private Map<String, String> author;
  private final List<Field> fields = new ArrayList<>();

  public DiscordEmbed title(String title) {
    this.title = title;
    return this;
  }

  public DiscordEmbed title(String title, String url) {
    this.title = title;
    this.url = url;
    return this;
  }

  public DiscordEmbed url(String url) {
    this.url = url;
    return this;
  }

  public DiscordEmbed description(String description) {
    this.description = description;
    return this;
  }

  public DiscordEmbed color(int rgb) {
    this.color = rgb & 0xFFFFFF;
    return this;
  }

  public DiscordEmbed color(java.awt.Color color) {
    if (color != null) {
      this.color = color.getRGB() & 0xFFFFFF;
    }
    return this;
  }

  public DiscordEmbed timestamp() {
    this.timestamp = Instant.now().toString();
    return this;
  }

  public DiscordEmbed timestamp(Instant instant) {
    if (instant != null) {
      this.timestamp = instant.toString();
    }
    return this;
  }

  public DiscordEmbed footer(String text) {
    return footer(text, null);
  }

  public DiscordEmbed footer(String text, String iconUrl) {
    this.footer = new HashMap<>();
    this.footer.put("text", text);
    if (iconUrl != null) {
      this.footer.put("icon_url", iconUrl);
    }
    return this;
  }

  public DiscordEmbed image(String url) {
    if (url != null) {
      this.image = Map.of("url", url);
    }
    return this;
  }

  public DiscordEmbed thumbnail(String url) {
    if (url != null) {
      this.thumbnail = Map.of("url", url);
    }
    return this;
  }

  public DiscordEmbed author(String name) {
    return author(name, null, null);
  }

  public DiscordEmbed author(String name, String iconUrl) {
    return author(name, null, iconUrl);
  }

  public DiscordEmbed author(String name, String url, String iconUrl) {
    this.author = new HashMap<>();
    this.author.put("name", name);
    if (url != null) this.author.put("url", url);
    if (iconUrl != null) this.author.put("icon_url", iconUrl);
    return this;
  }

  public DiscordEmbed field(String name, String value, boolean inline) {
    fields.add(new Field(name, value, inline));
    return this;
  }

  public DiscordEmbed field(String name, String value) {
    return field(name, value, false);
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    if (title != null) map.put("title", title);
    if (description != null) map.put("description", description);
    if (url != null) map.put("url", url);
    if (color != null) map.put("color", color);
    if (timestamp != null) map.put("timestamp", timestamp);
    if (footer != null) map.put("footer", footer);
    if (image != null) map.put("image", image);
    if (thumbnail != null) map.put("thumbnail", thumbnail);
    if (author != null) map.put("author", author);
    if (!fields.isEmpty()) {
      List<Map<String, Object>> fieldsList = new ArrayList<>();
      for (Field field : fields) {
        fieldsList.add(field.toMap());
      }
      map.put("fields", fieldsList);
    }
    return map;
  }
}
