package com.example.wsafety;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;

public class InstructionActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_instruction);

        Button gotItButton = findViewById(R.id.gotItbutton);

        gotItButton.setOnClickListener(v -> {
            Intent intent = new Intent(
                    InstructionActivity.this,
                    MainActivity.class
            );

            startActivity(intent);
            finish();
        });
    }
}