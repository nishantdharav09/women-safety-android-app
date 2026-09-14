package com.example.wsafety;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;

public class RegisterNumberActivity extends AppCompatActivity {

    private static final int MAX_CONTACTS = 5;

    private EditText[] editTextNumbers = new EditText[MAX_CONTACTS];
    private Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_number);

        // Emergency number fields
        editTextNumbers[0] = findViewById(R.id.numberEdit1);
        editTextNumbers[1] = findViewById(R.id.numberEdit2);
        editTextNumbers[2] = findViewById(R.id.numberEdit3);
        editTextNumbers[3] = findViewById(R.id.numberEdit4);
        editTextNumbers[4] = findViewById(R.id.numberEdit5);

        // Save button
        saveButton = findViewById(R.id.saveButton);

        // Previously saved numbers load करा
        loadSavedNumbers();

        // SAVE button click
        saveButton.setOnClickListener(v -> saveNumbers());
    }

    private void loadSavedNumbers() {

        SharedPreferences sharedPreferences =
                getSharedPreferences("MySharedPref", MODE_PRIVATE);

        for (int i = 0; i < MAX_CONTACTS; i++) {

            String number =
                    sharedPreferences.getString("ENUM" + i, "");

            if (editTextNumbers[i] != null) {
                editTextNumbers[i].setText(number);
            }
        }
    }

    private void saveNumbers() {

        SharedPreferences sharedPreferences =
                getSharedPreferences("MySharedPref", MODE_PRIVATE);

        SharedPreferences.Editor editor =
                sharedPreferences.edit();

        boolean hasNumber = false;

        for (int i = 0; i < MAX_CONTACTS; i++) {

            String number =
                    editTextNumbers[i]
                            .getText()
                            .toString()
                            .trim();

            if (!number.isEmpty()) {

                editor.putString(
                        "ENUM" + i,
                        number
                );

                hasNumber = true;

            } else {

                // रिकामी field असल्यास जुना number remove करा
                editor.putString(
                        "ENUM" + i,
                        ""
                );
            }
        }

        editor.apply();

        if (hasNumber) {

            Toast.makeText(
                    this,
                    "Emergency numbers saved!",
                    Toast.LENGTH_SHORT
            ).show();

            finish();

        } else {

            Toast.makeText(
                    this,
                    "Please enter at least one emergency number.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}