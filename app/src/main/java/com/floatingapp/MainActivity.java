package com.floatingapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.floatingapp.utils.SecurityManager;

public class MainActivity extends AppCompatActivity {

    private Button btnStart, btnStop;
    private TextView tvPermStatus;
    private SecurityManager secMgr;

    private final ActivityResultLauncher<Intent> overlayLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> updateUI());

    private final ActivityResultLauncher<Intent> lockLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), r -> {
            if (r.getResultCode() == RESULT_OK) {
                // Unlocked — proceed
            } else {
                finish();
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        secMgr   = new SecurityManager(this);
        btnStart = findViewById(R.id.btnStart);
        btnStop  = findViewById(R.id.btnStop);
        tvPermStatus = findViewById(R.id.tvPermStatus);

        // Check if we need to show lock screen to access the app
        if (secMgr.isSecurityEnabled() && secMgr.shouldAutoLock()) {
            Intent lock = new Intent(this, LockScreenActivity.class);
            lock.putExtra(LockScreenActivity.EXTRA_MODE, "unlock");
            lockLauncher.launch(lock);
        }

        findViewById(R.id.btnPermission).setOnClickListener(v ->
            overlayLauncher.launch(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()))));

        btnStart.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this)) {
                ContextCompat.startForegroundService(this,
                    new Intent(this, FloatingWindowService.class).setAction(FloatingWindowService.ACTION_START));
                moveTaskToBack(true);
                updateUI();
            } else {
                Toast.makeText(this, "Enable 'Display over other apps' first", Toast.LENGTH_LONG).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            startService(new Intent(this, FloatingWindowService.class).setAction(FloatingWindowService.ACTION_STOP));
            updateUI();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        secMgr.recordActivity();
        updateUI();
    }

    private void updateUI() {
        boolean hasPerm = Settings.canDrawOverlays(this);
        boolean running = FloatingWindowService.isRunning;

        tvPermStatus.setText(hasPerm ? "✓ Display over other apps: ENABLED" : "✗ Display over other apps: DISABLED");
        tvPermStatus.setTextColor(ContextCompat.getColor(this,
            hasPerm ? R.color.status_granted : R.color.status_denied));

        btnStart.setEnabled(hasPerm && !running);
        btnStop.setEnabled(running);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (id == R.id.action_security) {
            startActivity(new Intent(this, SecuritySettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
