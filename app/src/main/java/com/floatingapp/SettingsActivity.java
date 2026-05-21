package com.floatingapp;

import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.floatingapp.utils.AppPreferences;

public class SettingsActivity extends AppCompatActivity {

    private AppPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        prefs = new AppPreferences(this);

        // Window title
        EditText etTitle = findViewById(R.id.etWindowTitle);
        etTitle.setText(prefs.getString(AppPreferences.KEY_WINDOW_TITLE, "Floating Window"));

        // Default size
        Spinner spinSize = findViewById(R.id.spinSize);
        String[] sizes = {"small", "medium", "large"};
        spinSize.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, sizes));
        String currentSize = prefs.getString(AppPreferences.KEY_DEFAULT_SIZE, "medium");
        for (int i = 0; i < sizes.length; i++) {
            if (sizes[i].equals(currentSize)) { spinSize.setSelection(i); break; }
        }

        // Default corner
        Spinner spinCorner = findViewById(R.id.spinCorner);
        String[] corners = {"tl", "tr", "bl", "br"};
        String[] cornerLabels = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};
        spinCorner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cornerLabels));
        String currentCorner = prefs.getString(AppPreferences.KEY_DEFAULT_CORNER, "tl");
        for (int i = 0; i < corners.length; i++) {
            if (corners[i].equals(currentCorner)) { spinCorner.setSelection(i); break; }
        }

        // Checkboxes
        CheckBox cbAutoStart   = findViewById(R.id.cbAutoStart);
        CheckBox cbSnapToEdge  = findViewById(R.id.cbSnapToEdge);
        CheckBox cbHighContrast = findViewById(R.id.cbHighContrast);

        cbAutoStart.setChecked(prefs.getBoolean(AppPreferences.KEY_AUTO_START, false));
        cbSnapToEdge.setChecked(prefs.getBoolean(AppPreferences.KEY_SNAP_TO_EDGE, true));
        cbHighContrast.setChecked(prefs.getBoolean(AppPreferences.KEY_HIGH_CONTRAST, false));

        // Save button
        Button btnSave = findViewById(R.id.btnSaveSettings);
        btnSave.setOnClickListener(v -> {
            prefs.setString(AppPreferences.KEY_WINDOW_TITLE, etTitle.getText().toString().trim());
            prefs.setString(AppPreferences.KEY_DEFAULT_SIZE, sizes[spinSize.getSelectedItemPosition()]);
            prefs.setString(AppPreferences.KEY_DEFAULT_CORNER, corners[spinCorner.getSelectedItemPosition()]);
            prefs.setBoolean(AppPreferences.KEY_AUTO_START, cbAutoStart.isChecked());
            prefs.setBoolean(AppPreferences.KEY_SNAP_TO_EDGE, cbSnapToEdge.isChecked());
            prefs.setBoolean(AppPreferences.KEY_HIGH_CONTRAST, cbHighContrast.isChecked());
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
            finish();
        });

        // Reset button
        Button btnReset = findViewById(R.id.btnResetSettings);
        btnReset.setOnClickListener(v -> {
            prefs.setString(AppPreferences.KEY_WINDOW_TITLE, "Floating Window");
            prefs.setString(AppPreferences.KEY_DEFAULT_SIZE, "medium");
            prefs.setString(AppPreferences.KEY_DEFAULT_CORNER, "tl");
            prefs.setBoolean(AppPreferences.KEY_AUTO_START, false);
            prefs.setBoolean(AppPreferences.KEY_SNAP_TO_EDGE, true);
            prefs.setBoolean(AppPreferences.KEY_HIGH_CONTRAST, false);
            prefs.setInt(AppPreferences.KEY_WIN_X, -1);
            prefs.setInt(AppPreferences.KEY_WIN_Y, -1);
            Toast.makeText(this, "Settings reset!", Toast.LENGTH_SHORT).show();
            recreate();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
}
