package com.floatingapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;

import com.floatingapp.utils.SecurityManager;

public class SecuritySettingsActivity extends AppCompatActivity {

    private SecurityManager secMgr;

    private TextView tvCurrentStatus;
    private LinearLayout layoutSecurityOptions;
    private CheckBox cbBiometric, cbAutoLock, cbHideContent;
    private Spinner spinAutoLockTime;
    private Button btnSetPin, btnSetPattern, btnDisable;

    private final ActivityResultLauncher<Intent> lockLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            updateUI();
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        secMgr = new SecurityManager(this);

        tvCurrentStatus   = findViewById(R.id.tvSecurityStatus);
        layoutSecurityOptions = findViewById(R.id.layoutSecurityOptions);
        cbBiometric       = findViewById(R.id.cbBiometric);
        cbAutoLock        = findViewById(R.id.cbAutoLock);
        cbHideContent     = findViewById(R.id.cbHideContent);
        spinAutoLockTime  = findViewById(R.id.spinAutoLockTime);
        btnSetPin         = findViewById(R.id.btnSetPin);
        btnSetPattern     = findViewById(R.id.btnSetPattern);
        btnDisable        = findViewById(R.id.btnDisableSecurity);

        String[] times = {"30 seconds", "1 minute", "5 minutes", "15 minutes"};
        spinAutoLockTime.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, times));

        // Check biometric availability
        BiometricManager bm = BiometricManager.from(this);
        boolean biometricAvailable = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            == BiometricManager.BIOMETRIC_SUCCESS;
        cbBiometric.setEnabled(biometricAvailable);
        if (!biometricAvailable) cbBiometric.setText("Fingerprint/Face (not available on this device)");

        btnSetPin.setOnClickListener(v -> {
            Intent i = new Intent(this, LockScreenActivity.class);
            i.putExtra(LockScreenActivity.EXTRA_MODE, "setup_pin");
            lockLauncher.launch(i);
        });

        btnSetPattern.setOnClickListener(v -> {
            Intent i = new Intent(this, LockScreenActivity.class);
            i.putExtra(LockScreenActivity.EXTRA_MODE, "setup_pattern");
            lockLauncher.launch(i);
        });

        btnDisable.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                .setTitle("Disable Security")
                .setMessage("Are you sure you want to remove the lock from the floating window?")
                .setPositiveButton("Disable", (d, w) -> {
                    secMgr.disableSecurity();
                    Toast.makeText(this, "Security disabled.", Toast.LENGTH_SHORT).show();
                    updateUI();
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        Button btnSave = findViewById(R.id.btnSaveSecuritySettings);
        btnSave.setOnClickListener(v -> saveSettings());

        updateUI();
    }

    private void updateUI() {
        boolean enabled = secMgr.isSecurityEnabled();
        String type = secMgr.getSecurityType();

        if (enabled) {
            tvCurrentStatus.setText("🔒 Security enabled — " + (type.equals(SecurityManager.TYPE_PIN) ? "PIN" : "Pattern"));
            tvCurrentStatus.setTextColor(getColor(R.color.status_granted));
            layoutSecurityOptions.setVisibility(View.VISIBLE);
            btnDisable.setVisibility(View.VISIBLE);
        } else {
            tvCurrentStatus.setText("🔓 No security set");
            tvCurrentStatus.setTextColor(getColor(R.color.text_secondary));
            layoutSecurityOptions.setVisibility(View.GONE);
            btnDisable.setVisibility(View.GONE);
        }

        cbBiometric.setChecked(secMgr.isBiometricEnabled());
        cbAutoLock.setChecked(secMgr.isAutoLockEnabled());
        cbHideContent.setChecked(secMgr.isHideContentEnabled());

        int secs = secMgr.getAutoLockSeconds();
        int spinIdx = secs <= 30 ? 0 : secs <= 60 ? 1 : secs <= 300 ? 2 : 3;
        spinAutoLockTime.setSelection(spinIdx);
    }

    private void saveSettings() {
        int[] times = {30, 60, 300, 900};
        int autoLockSecs = times[spinAutoLockTime.getSelectedItemPosition()];
        secMgr.saveSettings(
            cbAutoLock.isChecked(),
            autoLockSecs,
            cbBiometric.isChecked(),
            cbHideContent.isChecked()
        );
        Toast.makeText(this, "Security settings saved!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
