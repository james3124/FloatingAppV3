package com.floatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.floatingapp.utils.SecurityManager;

import java.util.concurrent.Executor;

public class LockScreenActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode"; // "unlock" or "setup_pin" or "setup_pattern"
    public static final String EXTRA_VERIFY_CURRENT = "verify_current"; // true = verify before change

    private SecurityManager secMgr;
    private String mode = "unlock";

    // PIN UI
    private LinearLayout pinLayout;
    private TextView tvPinDots;
    private StringBuilder pinInput = new StringBuilder();
    private static final int PIN_LENGTH = 4;

    // Pattern UI  
    private LinearLayout patternLayout;
    private PatternView patternView;

    // Common
    private TextView tvTitle, tvSubtitle, tvError, tvLockout;
    private LinearLayout btnBiometric;

    // Setup flow: first entry stored here, second entry validates
    private String firstEntry = null;
    private boolean isConfirming = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_screen);

        secMgr = new SecurityManager(this);
        mode   = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = "unlock";

        tvTitle    = findViewById(R.id.tvLockTitle);
        tvSubtitle = findViewById(R.id.tvLockSubtitle);
        tvError    = findViewById(R.id.tvLockError);
        tvLockout  = findViewById(R.id.tvLockout);
        pinLayout  = findViewById(R.id.pinLayout);
        patternLayout = findViewById(R.id.patternLayout);
        btnBiometric  = findViewById(R.id.btnBiometric);

        setupUI();
    }

    private void setupUI() {
        // Lockout check
        if (secMgr.isLockedOut() && mode.equals("unlock")) {
            showLockoutTimer();
            return;
        }

        switch (mode) {
            case "setup_pin":
                setupPinSetup();
                break;
            case "setup_pattern":
                setupPatternSetup();
                break;
            case "unlock":
            default:
                setupUnlock();
                break;
        }
    }

    // ─── UNLOCK ───────────────────────────────────────────────────────────────

    private void setupUnlock() {
        String type = secMgr.getSecurityType();
        tvTitle.setText("Floating Window");
        tvSubtitle.setText("Enter your " + (type.equals(SecurityManager.TYPE_PIN) ? "PIN" : "pattern") + " to unlock");

        if (type.equals(SecurityManager.TYPE_PIN)) {
            showPinLayout();
            setupPinKeypad(pin -> {
                if (secMgr.verifyPin(pin)) {
                    secMgr.resetFailedAttempts();
                    secMgr.recordActivity();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    handleFailedAttempt();
                }
            });
        } else {
            showPatternLayout();
            setupPatternVerify(pattern -> {
                if (secMgr.verifyPattern(pattern)) {
                    secMgr.resetFailedAttempts();
                    secMgr.recordActivity();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    handleFailedAttempt();
                }
            });
        }

        // Biometric
        if (secMgr.isBiometricEnabled() && type.equals(SecurityManager.TYPE_PIN)) {
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> showBiometricPrompt());
            showBiometricPrompt(); // auto-show on open
        }
    }

    private void handleFailedAttempt() {
        secMgr.recordFailedAttempt();
        int attempts = secMgr.getFailedAttempts();
        int remaining = SecurityManager.MAX_ATTEMPTS_BEFORE_LOCKOUT - attempts;

        if (secMgr.isLockedOut()) {
            tvError.setVisibility(View.GONE);
            showLockoutTimer();
        } else {
            tvError.setVisibility(View.VISIBLE);
            tvError.setText("Incorrect. " + remaining + " attempt" + (remaining == 1 ? "" : "s") + " remaining.");
        }
        pinInput.setLength(0);
        updatePinDots();
    }

    private void showLockoutTimer() {
        pinLayout.setVisibility(View.GONE);
        patternLayout.setVisibility(View.GONE);
        btnBiometric.setVisibility(View.GONE);
        tvLockout.setVisibility(View.VISIBLE);
        tvTitle.setText("Too many attempts");
        tvSubtitle.setText("Try again after the countdown");

        new CountDownTimer(secMgr.getLockoutRemainingMs(), 1000) {
            public void onTick(long ms) {
                long secs = ms / 1000;
                tvLockout.setText("Locked for " + secs + "s");
            }
            public void onFinish() {
                tvLockout.setVisibility(View.GONE);
                secMgr.resetFailedAttempts();
                recreate();
            }
        }.start();
    }

    // ─── BIOMETRIC ────────────────────────────────────────────────────────────

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
            new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    secMgr.resetFailedAttempts();
                    secMgr.recordActivity();
                    setResult(RESULT_OK);
                    finish();
                }
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    // User cancelled or error — fall back to PIN
                }
                @Override
                public void onAuthenticationFailed() {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Biometric not recognized.");
                }
            });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Floating Window")
            .setSubtitle("Use your fingerprint or face")
            .setNegativeButtonText("Use PIN")
            .build();

        prompt.authenticate(info);
    }

    // ─── PIN SETUP ────────────────────────────────────────────────────────────

    private void setupPinSetup() {
        tvTitle.setText("Set PIN");
        tvSubtitle.setText("Enter a 4-digit PIN");
        showPinLayout();
        setupPinKeypad(pin -> {
            if (!isConfirming) {
                firstEntry = pin;
                isConfirming = true;
                pinInput.setLength(0);
                updatePinDots();
                tvSubtitle.setText("Confirm your PIN");
                tvError.setVisibility(View.GONE);
            } else {
                if (pin.equals(firstEntry)) {
                    secMgr.setPin(pin);
                    Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("PINs don't match. Try again.");
                    firstEntry = null;
                    isConfirming = false;
                    pinInput.setLength(0);
                    updatePinDots();
                    tvSubtitle.setText("Enter a 4-digit PIN");
                }
            }
        });
    }

    // ─── PATTERN SETUP ────────────────────────────────────────────────────────

    private void setupPatternSetup() {
        tvTitle.setText("Set Pattern");
        tvSubtitle.setText("Draw an unlock pattern");
        showPatternLayout();
        setupPatternInput(pattern -> {
            if (pattern.length() < 4) {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText("Pattern too short. Connect at least 4 dots.");
                return;
            }
            if (!isConfirming) {
                firstEntry = pattern;
                isConfirming = true;
                tvSubtitle.setText("Draw pattern again to confirm");
                tvError.setVisibility(View.GONE);
                if (patternView != null) patternView.clearPattern();
            } else {
                if (pattern.equals(firstEntry)) {
                    secMgr.setPattern(pattern);
                    Toast.makeText(this, "Pattern set successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    tvError.setVisibility(View.VISIBLE);
                    tvError.setText("Patterns don't match. Try again.");
                    firstEntry = null;
                    isConfirming = false;
                    tvSubtitle.setText("Draw an unlock pattern");
                    if (patternView != null) patternView.clearPattern();
                }
            }
        });
    }

    private void setupPatternVerify(OnPatternComplete callback) {
        setupPatternInput(callback);
    }

    // ─── PIN Keypad ───────────────────────────────────────────────────────────

    interface OnPinComplete { void onComplete(String pin); }
    interface OnPatternComplete { void onComplete(String pattern); }

    private OnPinComplete pinCallback;
    private TextView tvPinDotsRef;

    private void showPinLayout() {
        pinLayout.setVisibility(View.VISIBLE);
        patternLayout.setVisibility(View.GONE);
        tvPinDots = findViewById(R.id.tvPinDots);
        tvPinDotsRef = tvPinDots;
        updatePinDots();
    }

    private void showPatternLayout() {
        pinLayout.setVisibility(View.GONE);
        patternLayout.setVisibility(View.VISIBLE);
    }

    private void setupPinKeypad(OnPinComplete callback) {
        this.pinCallback = callback;
        int[] btnIds = {
            R.id.pin0, R.id.pin1, R.id.pin2, R.id.pin3, R.id.pin4,
            R.id.pin5, R.id.pin6, R.id.pin7, R.id.pin8, R.id.pin9
        };
        for (int i = 0; i < btnIds.length; i++) {
            final String digit = String.valueOf(i);
            Button btn = findViewById(btnIds[i]);
            if (btn != null) btn.setOnClickListener(v -> appendPin(digit));
        }
        Button btnDel = findViewById(R.id.pinDel);
        if (btnDel != null) btnDel.setOnClickListener(v -> deletePin());
    }

    private void appendPin(String digit) {
        if (pinInput.length() >= PIN_LENGTH) return;
        pinInput.append(digit);
        updatePinDots();
        if (pinInput.length() == PIN_LENGTH) {
            String pin = pinInput.toString();
            pinInput.setLength(0);
            updatePinDots();
            if (pinCallback != null) pinCallback.onComplete(pin);
        }
    }

    private void deletePin() {
        if (pinInput.length() > 0) {
            pinInput.deleteCharAt(pinInput.length() - 1);
            updatePinDots();
        }
    }

    private void updatePinDots() {
        if (tvPinDotsRef == null) return;
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < PIN_LENGTH; i++) {
            dots.append(i < pinInput.length() ? "⬤ " : "○ ");
        }
        tvPinDotsRef.setText(dots.toString().trim());
    }

    private void setupPatternInput(OnPatternComplete callback) {
        patternView = findViewById(R.id.patternView);
        if (patternView != null) {
            patternView.setOnPatternCompleteListener(callback::onComplete);
        }
    }

    @Override public void onBackPressed() {
        if (mode.equals("unlock")) {
            // Can't go back from lock screen — minimize app instead
            moveTaskToBack(true);
        } else {
            super.onBackPressed();
        }
    }
}
