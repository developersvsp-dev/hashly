package com.vaibhav.hashly;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class DashboardActivity extends AppCompatActivity {
    private TextView welcomeText, userEmail;
    private Button btnLogout, btnGetHashtags;
    private GoogleSignInClient mGoogleSignInClient;

    // 🔥 UPDATED: Navigate to HashtagsDisplayActivity after niche selection
    private ActivityResultLauncher<Intent> nicheLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String niche = result.getData().getStringExtra("niche");

                    // 🔥 SHOW HASHTAGS TO USER - Navigate to display screen
                    Intent hashtagsIntent = new Intent(DashboardActivity.this, HashtagsDisplayActivity.class);
                    hashtagsIntent.putExtra("niche", niche);
                    startActivity(hashtagsIntent);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize Google Sign-In client
        setupGoogleSignIn();

        // Initialize ALL UI elements
        welcomeText = findViewById(R.id.welcomeText);
        userEmail = findViewById(R.id.userEmail);
        btnLogout = findViewById(R.id.btnLogout);
        btnGetHashtags = findViewById(R.id.btnGetHashtags);

        // 🔥 HASHTAGS BUTTON - Navigate to Niche Selector
        btnGetHashtags.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, NicheSelectorActivity.class);
            nicheLauncher.launch(intent);
        });

        // 🔥 LOGOUT BUTTON
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                Toast.makeText(this, "Signed out successfully! 👋", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(DashboardActivity.this, MainActivity.class));
                finish();
            });
        });

        // 🔥 SET USER DATA
        String userName = getIntent().getStringExtra("user_name");
        String userEmailText = getIntent().getStringExtra("user_email");

        if (userName != null) {
            welcomeText.setText("Welcome to Hashly, " + userName + "!");
        } else {
            welcomeText.setText("Welcome to Hashly! 🚀");
        }

        if (userEmailText != null) {
            userEmail.setText(userEmailText);
        }
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }
}
