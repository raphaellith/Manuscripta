package com.manuscripta.student.ui.main;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.manuscripta.student.databinding.ActivityMainBinding;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Main activity for the Manuscripta Student application.
 */
@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    /**
     * View binding for the main activity layout.
     */
    private ActivityMainBinding binding;

    /**
     * ViewModel for the main activity.
     */
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);

        setupUI();
    }

    private void setupUI() {
        binding.textView.setText("Hello, Manuscripta!");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
