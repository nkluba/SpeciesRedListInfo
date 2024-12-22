package com.example.speciesredlistinfo;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    // Declare UI elements
    private EditText speciesInput;
    private Button fetchButton;
    private TableLayout speciesInfoTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set gesture insets and layout
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

        // Set event listener for fetch button
        fetchButton.setOnClickListener(v -> fetchSpeciesInfo());
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

                        // Extract the required fields from "details"
                        String populationTrend = details.optString("populationtrend", "N/A");
                        String rationale = details.optString("rationale", "N/A");
                        String geographicRange = details.optString("geographicrange", "N/A");
                        String habitat = details.optString("habitat", "N/A");
                        String threats = details.optString("threats", "N/A");
                        String conservationMeasures = details.optString("conservationmeasures", "N/A");

                        // Update the UI with parsed data
                        runOnUiThread(() -> {
                            clearTable(); // Clear any previous content
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

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextSize(16);
        labelView.setPadding(8, 8, 8, 8);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextSize(16);
        valueView.setPadding(8, 8, 8, 8);

        // Add label and value to the row
        row.addView(labelView);
        row.addView(valueView);

        // Add the row to the table
        speciesInfoTable.addView(row);
    }
}