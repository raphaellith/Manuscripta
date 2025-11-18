package com.manuscripta.student.ui.main;

import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for MainViewModel.
 */
public class MainViewModelTest {

    private MainViewModel viewModel;

    @Before
    public void setUp() {
        viewModel = new MainViewModel();
    }

    @Test
    public void testViewModelCreation() {
        assertNotNull(viewModel);
    }
}
