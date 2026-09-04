package dev.mukulx.javaskript.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Produces stable, path-safe storage identifiers for script-owned files. */
public final class ScriptStorage {

  private ScriptStorage() {}

  public static String id(String scriptKey) {
    if (scriptKey == null || scriptKey.isBlank() || scriptKey.contains("..")) {
      throw new IllegalArgumentException("Invalid script key: " + scriptKey);
    }
    String normalized = scriptKey.replace('\\', '/').replaceAll("\\.java$", "");
    if (!normalized.contains("/")) {
      return normalized;
    }
    String readable = normalized.replaceAll("[^A-Za-z0-9._-]", "_");
    return readable + "-" + shortHash(normalized);
  }

  private static String shortHash(String value) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder(12);
      for (int i = 0; i < 6; i++) result.append(String.format("%02x", hash[i]));
      return result.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 is unavailable", e);
    }
  }
}
