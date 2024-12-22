package com.example.speciesredlistinfo;

import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Declare UI elements
    private EditText speciesInput;
    private Button fetchButton;
    private TableLayout speciesInfoTable;
    private SeekBar fontSizeSeekBar;
    private SwitchCompat darkModeSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable edge-to-edge layout and apply system insets
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI elements
        speciesInput = findViewById(R.id.speciesInput);
        fetchButton = findViewById(R.id.fetchButton);
        speciesInfoTable = findViewById(R.id.speciesInfoTable);
        fontSizeSeekBar = findViewById(R.id.fontSizeSeekBar);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        Button saveButton = findViewById(R.id.saveButton);

        // Prepare Dark Mode Toggle
        boolean isDarkModeEnabled = getPreferences(MODE_PRIVATE).getBoolean("dark_mode", false);
        darkModeSwitch.setChecked(isDarkModeEnabled);
        setDarkMode(isDarkModeEnabled);

        // Set event listeners
        fetchButton.setOnClickListener(v -> fetchSpeciesInfo());
        saveButton.setOnClickListener(v -> saveJsonData());

        fontSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adjustFontSize(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Set default font size
        adjustFontSize(fontSizeSeekBar.getProgress());
    }

    /**
     * Save the fetched JSON response to a file in the external storage.
     */
    private void saveJsonData() {
        new Thread(() -> {
            try {
                // Retrieve data from the last fetched Species Info
                String speciesName = speciesInput.getText().toString().trim();
                String result = DataFetcher.fetchSpeciesInfo(speciesName);

                if (result != null && !result.isEmpty()) {
                    // Parse and save the JSON response
                    JSONObject jsonResponse = new JSONObject(result);

                    // Save the JSON response to a file
                    File file = new File(getExternalFilesDir(null), "species_info.json");
                    try (FileWriter writer = new FileWriter(file)) {
                        writer.write(jsonResponse.toString(4)); // Write JSON data with indentation
                    }

                    // Provide feedback to the user
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "JSON saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show());
                } else {
                    // Handle empty response
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "No data to save!", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to save JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    /**
     * Applies Dark Mode or Light Mode depending on the user's choice.
     *
     * @param isEnabled true if Dark Mode should be enabled, false otherwise.
     */
    private void setDarkMode(boolean isEnabled) {
        if (isEnabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private String cleanHtml(String rawHtml) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY).toString().trim();
        } else {
            return Html.fromHtml(rawHtml).toString().trim();
        }
    }

    /**
     * Adjusts the font size of all rows in the TableLayout based on the provided size.
     *
     * @param fontSize The size to set for the font.
     */
    private void adjustFontSize(int fontSize) {
        for (int i = 0; i < speciesInfoTable.getChildCount(); i++) {
            TableRow row = (TableRow) speciesInfoTable.getChildAt(i);

            for (int j = 0; j < row.getChildCount(); j++) {
                // Only update TextView elements
                if (row.getChildAt(j) instanceof TextView) {
                    TextView textView = (TextView) row.getChildAt(j);
                    textView.setTextSize(fontSize); // Adjust font size dynamically
                }
            }
        }
    }

    /**
     * Fetch and display species information based on user input.
     */
    private void fetchSpeciesInfo() {
        String speciesName = speciesInput.getText().toString().trim();

        // Validate input
        if (speciesName.isEmpty()) {
            clearTable();
            addRowToTable("Error", "Please enter a valid scientific name.");
            return;
        }

        // Notify user and clear previous content
        clearTable();
        addRowToTable("Info", "Fetching species info...");

        // Perform API call on a background thread to avoid blocking the UI
        new Thread(() -> {
            try {
                // Fetch data using the DataFetcher class
                String result = DataFetcher.fetchSpeciesInfo(speciesName);

                if (result != null && !result.isEmpty()) {
                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(result);

                    // Extract root-level "name" field
                    String name = jsonResponse.optString("name", "N/A");

                    // Extract first entry in "result" array
                    JSONArray resultsArray = jsonResponse.optJSONArray("result");

                    if (resultsArray != null && resultsArray.length() > 0) {
                        JSONObject details = resultsArray.getJSONObject(0);

                        // Extract the required fields from "details", clean HTML from each field
                        String populationTrend = cleanHtml(details.optString("populationtrend", "N/A"));
                        String rationale = cleanHtml(details.optString("rationale", "N/A"));
                        String geographicRange = cleanHtml(details.optString("geographicrange", "N/A"));
                        String habitat = cleanHtml(details.optString("habitat", "N/A"));
                        String threats = cleanHtml(details.optString("threats", "N/A"));
                        String conservationMeasures = cleanHtml(details.optString("conservationmeasures", "N/A"));

                        // Update the UI with parsed and cleaned data
                        runOnUiThread(() -> {
                            clearTable(); // Clear existing rows
                            addRowToTable("Name", name);
                            addRowToTable("Population Trend", populationTrend);
                            addRowToTable("Rationale", rationale);
                            addRowToTable("Geographic Range", geographicRange);
                            addRowToTable("Habitat", habitat);
                            addRowToTable("Threats", threats);
                            addRowToTable("Conservation Measures", conservationMeasures);
                        });
                    } else {
                        // Handle case where "result" array is empty or missing
                        runOnUiThread(() -> {
                            clearTable();
                            addRowToTable("Error", "No valid data available for the species.");
                        });
                    }
                } else {
                    // Handle null or empty API response
                    runOnUiThread(() -> {
                        clearTable();
                        addRowToTable("Error", "No data available for the species: " + speciesName);
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error fetching species info", e);

                // Show error on UI thread
                runOnUiThread(() -> {
                    clearTable();
                    String errorMessage = (e.getMessage() != null) ? e.getMessage() : "Unknown error occurred.";
                    addRowToTable("Error", errorMessage);
                });
            }
        }).start();
    }

    /**
     * Clears all data from the species info table.
     */
    private void clearTable() {
        speciesInfoTable.removeAllViews();
    }

    /**
     * Adds a row with a label and value to the species info table.
     *
     * @param label Field label.
     * @param value Field value.
     */
    private void addRowToTable(String label, String value) {
        TableRow row = new TableRow(this);

        // Create the label TextView (left column)
        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setPadding(8, 8, 8, 8);
        labelView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1f)); // Use weight-based layout
        labelView.setSingleLine(false); // Enable text wrapping
        labelView.setEllipsize(null); // Disable ellipsis (truncation)

        // Create the value TextView (right column)
        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setPadding(8, 8, 8, 8);
        valueView.setLayoutParams(new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 2f)); // Use weight-based layout
        valueView.setSingleLine(false); // Enable text wrapping for long text
        valueView.setEllipsize(null); // Disable ellipsis (truncation)

        // Add the label and value TextViews to the row
        row.addView(labelView);
        row.addView(valueView);

        // Add the row to the table
        speciesInfoTable.addView(row);
    }
}