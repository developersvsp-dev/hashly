package com.vaibhav.hashly;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "GoogleSignIn";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;

    // UI Elements - Updated for clean login screen only
    private TextView statusText, signInText, signOutText;
    private CardView googleCard, signOutCard;

    // 🔥 MODERN ActivityResultLauncher
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI elements (matches clean XML)
        statusText = findViewById(R.id.statusText);
        googleCard = findViewById(R.id.googleCard);
        signInText = findViewById(R.id.btnGoogleSignIn);
        signOutCard = findViewById(R.id.signOutCard);
        signOutText = findViewById(R.id.btnSignOut);

        // 🔥 SETUP Google Sign-In
        setupGoogleSignIn();

        // Check current auth state
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);

        // 🔥 Click listeners
        googleCard.setOnClickListener(v -> {
            Log.d("HashlyDebug", "🔥 GOOGLE CARD TAPPED!");
            Toast.makeText(this, "Opening Google Sign-In...", Toast.LENGTH_SHORT).show();
            signIn();
        });

        signOutCard.setOnClickListener(v -> {
            Log.d("HashlyDebug", "🔥 SIGN OUT TAPPED!");
            signOut();
        });
    }

    private void setupGoogleSignIn() {
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    Log.d("HashlyDebug", "Google result: " + result.getResultCode());
                    if (result.getResultCode() == RESULT_OK) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "✅ Google success: " + account.getEmail());
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.w(TAG, "❌ Google failed: " + e.getStatusCode());
                            Toast.makeText(this, "Sign-In failed: " + e.getStatusCode(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.w(TAG, "❌ Sign-In cancelled");
                        Toast.makeText(this, "Sign-In cancelled", Toast.LENGTH_SHORT).show();
                    }
                });

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void signIn() {
        Log.d("HashlyDebug", "🔥 Starting Google Sign-In");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        Log.d("HashlyDebug", "🔥 Firebase auth with Google token");
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "✅ Firebase auth SUCCESS!");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // 🔥 NAVIGATE TO DASHBOARD
                        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                        intent.putExtra("user_name", user.getDisplayName());
                        intent.putExtra("user_email", user.getEmail());
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish(); // Close login screen
                    } else {
                        Log.w(TAG, "❌ Firebase auth FAILED", task.getException());
                        Toast.makeText(this, "Firebase auth failed", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void signOut() {
        Log.d("HashlyDebug", "🔥 Signing out...");
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Log.d(TAG, "✅ Sign-out complete");
            updateUI(null);
        });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // 🔥 User signed in - Go to dashboard immediately
            Intent intent = new Intent(this, DashboardActivity.class);
            intent.putExtra("user_name", user.getDisplayName());
            startActivity(intent);
            finish();
        } else {
            // 🔥 Show clean login screen
            statusText.setText("Welcome Back!");
            googleCard.setVisibility(View.VISIBLE);
            signOutCard.setVisibility(View.GONE);
        }
    }
}
