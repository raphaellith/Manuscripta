package com.manuscripta.student.ui.pairing;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.manuscripta.student.databinding.ActivityPairingBinding;
import com.manuscripta.student.ui.main.MainActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Activity for the device pairing screen.
 *
 * <p>Coordinates the full pairing flow: UDP discovery → TCP handshake
 * → HTTP registration. On success, navigates to {@link MainActivity}.</p>
 *
 * <p>Per Pairing Process §2 and Android System Design §8, this is the
 * first screen shown when the app launches and the device is not yet
 * paired.</p>
 */
@AndroidEntryPoint
public class PairingActivity extends AppCompatActivity {

    /** View binding for the pairing layout. */
    private ActivityPairingBinding binding;

    /** ViewModel orchestrating the pairing flow. */
    private PairingViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPairingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(PairingViewModel.class);

        setupButtons();
        observeViewModel();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }

    /**
     * Sets up button click listeners for pairing and retry actions.
     */
    private void setupButtons() {
        binding.pairButton.setOnClickListener(v -> {
            String name = binding.deviceNameInput.getText().toString().trim();
            if (name.isEmpty()) {
                binding.deviceNameInput.setError("Please enter your name");
                return;
            }
            viewModel.startPairing(name);
        });

        binding.retryButton.setOnClickListener(v -> viewModel.retry());
    }

    /**
     * Observes ViewModel LiveData to update the UI based on pairing state.
     */
    private void observeViewModel() {
        viewModel.getStatusMessage().observe(this, status -> {
            if (binding != null) {
                binding.statusText.setText(status);
            }
        });

        viewModel.getErrorMessage().observe(this, error -> {
            if (binding != null) {
                if (error != null) {
                    binding.errorText.setText(error);
                    binding.errorText.setVisibility(View.VISIBLE);
                } else {
                    binding.errorText.setVisibility(View.GONE);
                }
            }
        });

        viewModel.getPairingPhase().observe(this, this::updateUiForPhase);

        viewModel.getPairingComplete().observe(this, complete -> {
            if (Boolean.TRUE.equals(complete)) {
                navigateToMain();
            }
        });
    }

    /**
     * Updates the UI elements based on the current pairing phase.
     *
     * @param phase The current pairing phase
     */
    private void updateUiForPhase(PairingPhase phase) {
        if (binding == null) {
            return;
        }

        switch (phase) {
            case IDLE:
                binding.pairButton.setVisibility(View.VISIBLE);
                binding.pairButton.setEnabled(true);
                binding.retryButton.setVisibility(View.GONE);
                binding.progressText.setVisibility(View.GONE);
                binding.deviceNameInput.setEnabled(true);
                break;

            case DISCOVERING:
            case TCP_PAIRING:
            case HTTP_REGISTERING:
                binding.pairButton.setVisibility(View.VISIBLE);
                binding.pairButton.setEnabled(false);
                binding.retryButton.setVisibility(View.GONE);
                binding.progressText.setVisibility(View.VISIBLE);
                binding.progressText.setText(getProgressText(phase));
                binding.deviceNameInput.setEnabled(false);
                break;

            case PAIRED:
                binding.pairButton.setVisibility(View.GONE);
                binding.retryButton.setVisibility(View.GONE);
                binding.progressText.setVisibility(View.GONE);
                binding.deviceNameInput.setEnabled(false);
                break;

            case ERROR:
                binding.pairButton.setVisibility(View.GONE);
                binding.retryButton.setVisibility(View.VISIBLE);
                binding.progressText.setVisibility(View.GONE);
                binding.deviceNameInput.setEnabled(true);
                break;

            default:
                break;
        }
    }

    /**
     * Returns a progress indicator string for the given phase.
     *
     * @param phase The active pairing phase
     * @return A progress text string
     */
    private String getProgressText(PairingPhase phase) {
        switch (phase) {
            case DISCOVERING:
                return "● ○ ○";
            case TCP_PAIRING:
                return "● ● ○";
            case HTTP_REGISTERING:
                return "● ● ●";
            default:
                return "";
        }
    }

    /**
     * Navigates to the main activity after successful pairing.
     */
    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
