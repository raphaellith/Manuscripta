package com.manuscripta.student.ui.reading;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.manuscripta.student.data.model.MaterialType;
import com.manuscripta.student.data.repository.MaterialRepository;
import com.manuscripta.student.domain.model.Material;
import com.manuscripta.student.utils.UiState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link ReadingViewModel}.
 */
public class ReadingViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private MaterialRepository mockMaterialRepository;

    private ReadingViewModel viewModel;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        viewModel = new ReadingViewModel(mockMaterialRepository);
    }

    @Test
    public void testInitialStateIsLoading() {
        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading());
    }

    @Test
    public void testSetMaterial() {
        Material material = createTestMaterial("mat-1", "Test Material");

        viewModel.setMaterial(material);

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertTrue(state.isSuccess());
        assertNotNull(state.getData());
        assertEquals("mat-1", state.getData().getId());
        assertEquals("Test Material", state.getData().getTitle());
    }

    @Test
    public void testLoadMaterialSuccess() {
        Material material = createTestMaterial("mat-2", "Loaded Material");
        when(mockMaterialRepository.getMaterialById("mat-2")).thenReturn(material);

        viewModel.loadMaterial("mat-2");

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertTrue(state.isSuccess());
        assertEquals("mat-2", state.getData().getId());
    }

    @Test
    public void testLoadMaterialNotFound() {
        when(mockMaterialRepository.getMaterialById("missing")).thenReturn(null);

        viewModel.loadMaterial("missing");

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertTrue(state.isError());
        assertEquals("Material not found", state.getErrorMessage());
    }

    @Test
    public void testSetLoading() {
        Material material = createTestMaterial("mat-1", "Title");
        viewModel.setMaterial(material);

        viewModel.setLoading();

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertTrue(state.isLoading());
    }

    @Test
    public void testLoadMaterialTransitionsFromLoadingToSuccess() {
        Material material = createTestMaterial("mat-3", "Title");
        when(mockMaterialRepository.getMaterialById("mat-3")).thenReturn(material);

        viewModel.loadMaterial("mat-3");

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertTrue(state.isSuccess());
    }

    @Test
    public void testLoadMaterialTransitionsFromLoadingToError() {
        when(mockMaterialRepository.getMaterialById("absent")).thenReturn(null);

        viewModel.loadMaterial("absent");

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertFalse(state.isLoading());
        assertTrue(state.isError());
    }

    @Test
    public void testSetMaterialOverwritesPrevious() {
        Material first = createTestMaterial("mat-1", "First");
        Material second = createTestMaterial("mat-2", "Second");

        viewModel.setMaterial(first);
        viewModel.setMaterial(second);

        UiState<Material> state = viewModel.getMaterialState().getValue();
        assertNotNull(state);
        assertEquals("mat-2", state.getData().getId());
    }

    @Test
    public void testGetMaterialStateReturnsNonNull() {
        assertNotNull(viewModel.getMaterialState());
    }

    @Test
    public void testSuccessStateDataProperties() {
        Material material = new Material(
                "id-123", MaterialType.WORKSHEET, "Worksheet Title",
                "Content body", "{\"key\":\"value\"}", "[\"term1\"]", 1000L);

        viewModel.setMaterial(material);

        UiState<Material> state = viewModel.getMaterialState().getValue();
        Material data = state.getData();
        assertNotNull(data);
        assertEquals("id-123", data.getId());
        assertEquals(MaterialType.WORKSHEET, data.getType());
        assertEquals("Worksheet Title", data.getTitle());
        assertEquals("Content body", data.getContent());
    }

    private Material createTestMaterial(String id, String title) {
        return new Material(id, MaterialType.READING, title,
                "Content text", "{}", "[]", System.currentTimeMillis());
    }
}
