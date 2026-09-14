package com.example.wsafety;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.womensafety.R;

public class EmergencyNumbersActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.emergency_numbers_layout);

        Button policeButton = findViewById(R.id.police_button);
        Button womenHelpline = findViewById(R.id.womenhelpline);
        Button fireButton = findViewById(R.id.fire_button);
        Button ambulanceButton = findViewById(R.id.ambulance_button);
        Button womenHelplineDA = findViewById(R.id.women_button);

        if (policeButton != null) policeButton.setOnClickListener(v -> callEmergencyNumber("100"));
        if (womenHelpline != null) womenHelpline.setOnClickListener(v -> callEmergencyNumber("1091"));
        if (fireButton != null) fireButton.setOnClickListener(v -> callEmergencyNumber("101"));
        if (ambulanceButton != null) ambulanceButton.setOnClickListener(v -> callEmergencyNumber("108"));
        if (womenHelplineDA != null) womenHelplineDA.setOnClickListener(v -> callEmergencyNumber("181"));
    }

    private void callEmergencyNumber(String phoneNumber) {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:" + phoneNumber));
        startActivity(intent);
        finish(); // Close this activity after dialing
    }
}
