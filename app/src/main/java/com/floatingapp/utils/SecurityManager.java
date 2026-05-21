package com.floatingapp.utils;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityManager {

    private static final String PREF_NAME = "FloatingAppSecurity";

    // Keys
    public static final String KEY_SECURITY_ENABLED  = "security_enabled";
    public static final String KEY_SECURITY_TYPE      = "security_type"; // pin / pattern / none
    public static final String KEY_PIN_HASH           = "pin_hash";
    public static final String KEY_PIN_SALT           = "pin_salt";
    public static final String KEY_PATTERN_HASH       = "pattern_hash";
    public static final String KEY_PATTERN_SALT       = "pattern_salt";
    public static final String KEY_BIOMETRIC_ENABLED  = "biometric_enabled";
    public static final String KEY_AUTO_LOCK_ENABLED  = "auto_lock_enabled";
    public static final String KEY_AUTO_LOCK_SECONDS  = "auto_lock_seconds"; // 30/60/300
    public static final String KEY_LAST_ACTIVE_TIME   = "last_active_time";
    public static final String KEY_FAILED_ATTEMPTS    = "failed_attempts";
    public static final String KEY_LOCKED_UNTIL       = "locked_until";
    public static final String KEY_HIDE_CONTENT       = "hide_content_on_lock"; // blur/hide content

    public static final String TYPE_NONE    = "none";
    public static final String TYPE_PIN     = "pin";
    public static final String TYPE_PATTERN = "pattern";

    // Lockout: 5 failed attempts → 30s lockout, doubles each time
    public static final int MAX_ATTEMPTS_BEFORE_LOCKOUT = 5;
    public static final long BASE_LOCKOUT_MS = 30_000L;

    private final android.content.SharedPreferences prefs;

    public SecurityManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // ─── Enable / disable ─────────────────────────────────────────────────────

    public boolean isSecurityEnabled() {
        return prefs.getBoolean(KEY_SECURITY_ENABLED, false);
    }

    public String getSecurityType() {
        return prefs.getString(KEY_SECURITY_TYPE, TYPE_NONE);
    }

    public boolean isBiometricEnabled() {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false);
    }

    public void setBiometricEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply();
    }

    public boolean isAutoLockEnabled() {
        return prefs.getBoolean(KEY_AUTO_LOCK_ENABLED, false);
    }

    public int getAutoLockSeconds() {
        return prefs.getInt(KEY_AUTO_LOCK_SECONDS, 60);
    }

    public boolean isHideContentEnabled() {
        return prefs.getBoolean(KEY_HIDE_CONTENT, true);
    }

    // ─── PIN ──────────────────────────────────────────────────────────────────

    public void setPin(String pin) {
        String salt = generateSalt();
        String hash = hashWithSalt(pin, salt);
        prefs.edit()
            .putBoolean(KEY_SECURITY_ENABLED, true)
            .putString(KEY_SECURITY_TYPE, TYPE_PIN)
            .putString(KEY_PIN_SALT, salt)
            .putString(KEY_PIN_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply();
    }

    public boolean verifyPin(String pin) {
        String salt = prefs.getString(KEY_PIN_SALT, "");
        String stored = prefs.getString(KEY_PIN_HASH, "");
        return stored.equals(hashWithSalt(pin, salt));
    }

    // ─── Pattern ──────────────────────────────────────────────────────────────

    /** pattern is a string like "0-1-2-5-8" (node indices 0-8) */
    public void setPattern(String pattern) {
        String salt = generateSalt();
        String hash = hashWithSalt(pattern, salt);
        prefs.edit()
            .putBoolean(KEY_SECURITY_ENABLED, true)
            .putString(KEY_SECURITY_TYPE, TYPE_PATTERN)
            .putString(KEY_PATTERN_SALT, salt)
            .putString(KEY_PATTERN_HASH, hash)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply();
    }

    public boolean verifyPattern(String pattern) {
        String salt = prefs.getString(KEY_PATTERN_SALT, "");
        String stored = prefs.getString(KEY_PATTERN_HASH, "");
        return stored.equals(hashWithSalt(pattern, salt));
    }

    // ─── Remove security ──────────────────────────────────────────────────────

    public void disableSecurity() {
        prefs.edit()
            .putBoolean(KEY_SECURITY_ENABLED, false)
            .putString(KEY_SECURITY_TYPE, TYPE_NONE)
            .remove(KEY_PIN_HASH).remove(KEY_PIN_SALT)
            .remove(KEY_PATTERN_HASH).remove(KEY_PATTERN_SALT)
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .apply();
    }

    // ─── Failed attempts / lockout ────────────────────────────────────────────

    public void recordFailedAttempt() {
        int attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, attempts).apply();
        if (attempts >= MAX_ATTEMPTS_BEFORE_LOCKOUT) {
            long multiplier = (long) Math.pow(2, (attempts - MAX_ATTEMPTS_BEFORE_LOCKOUT));
            long lockDuration = Math.min(BASE_LOCKOUT_MS * multiplier, 15 * 60 * 1000L); // max 15 min
            long lockedUntil = System.currentTimeMillis() + lockDuration;
            prefs.edit().putLong(KEY_LOCKED_UNTIL, lockedUntil).apply();
        }
    }

    public void resetFailedAttempts() {
        prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKED_UNTIL, 0).apply();
    }

    public int getFailedAttempts() {
        return prefs.getInt(KEY_FAILED_ATTEMPTS, 0);
    }

    public boolean isLockedOut() {
        long lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0);
        return System.currentTimeMillis() < lockedUntil;
    }

    public long getLockoutRemainingMs() {
        long lockedUntil = prefs.getLong(KEY_LOCKED_UNTIL, 0);
        return Math.max(0, lockedUntil - System.currentTimeMillis());
    }

    // ─── Auto-lock ────────────────────────────────────────────────────────────

    public void recordActivity() {
        prefs.edit().putLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis()).apply();
    }

    public boolean shouldAutoLock() {
        if (!isSecurityEnabled() || !isAutoLockEnabled()) return false;
        long lastActive = prefs.getLong(KEY_LAST_ACTIVE_TIME, System.currentTimeMillis());
        long elapsed = System.currentTimeMillis() - lastActive;
        return elapsed > (getAutoLockSeconds() * 1000L);
    }

    // ─── Save settings ────────────────────────────────────────────────────────

    public void saveSettings(boolean autoLock, int autoLockSeconds, boolean biometric, boolean hideContent) {
        prefs.edit()
            .putBoolean(KEY_AUTO_LOCK_ENABLED, autoLock)
            .putInt(KEY_AUTO_LOCK_SECONDS, autoLockSeconds)
            .putBoolean(KEY_BIOMETRIC_ENABLED, biometric)
            .putBoolean(KEY_HIDE_CONTENT, hideContent)
            .apply();
    }

    // ─── Crypto helpers ───────────────────────────────────────────────────────

    private String generateSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    private String hashWithSalt(String input, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            return "";
        }
    }
}
