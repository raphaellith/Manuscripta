package com.manuscripta.student.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.manuscripta.student.data.local.DeviceStatusDao;
import com.manuscripta.student.data.model.DeviceStatus;
import com.manuscripta.student.data.model.DeviceStatusEntity;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link DeviceStatusRepositoryImpl}.
 */
public class DeviceStatusRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Mock
    private DeviceStatusDao mockDao;

    private DeviceStatusRepositoryImpl repository;

    private static final String TEST_DEVICE_ID = "test-device-123";
    private static final String TEST_MATERIAL_ID = "material-456";
    private static final int TEST_BATTERY_LEVEL = 75;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new DeviceStatusRepositoryImpl(mockDao);
    }

    // ========== Constructor tests ==========

    @Test
    public void constructor_createsInstance() {
        assertNotNull(repository);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_nullDao_throwsException() {
        new DeviceStatusRepositoryImpl(null);
    }

    // ========== getDeviceStatus tests ==========

    @Test
    public void getDeviceStatus_existingDevice_returnsStatus() {
        DeviceStatusEntity entity = createTestEntity(TEST_DEVICE_ID, DeviceStatus.ON_TASK);
        when(mockDao.getById(TEST_DEVICE_ID)).thenReturn(entity);

        com.manuscripta.student.domain.model.DeviceStatus result =
                repository.getDeviceStatus(TEST_DEVICE_ID);

        assertNotNull(result);
        assertEquals(TEST_DEVICE_ID, result.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, result.getStatus());
    }

    @Test
    public void getDeviceStatus_nonExistingDevice_returnsNull() {
        when(mockDao.getById(TEST_DEVICE_ID)).thenReturn(null);

        com.manuscripta.student.domain.model.DeviceStatus result =
                repository.getDeviceStatus(TEST_DEVICE_ID);

        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeviceStatus_nullDeviceId_throwsException() {
        repository.getDeviceStatus(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeviceStatus_emptyDeviceId_throwsException() {
        repository.getDeviceStatus("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getDeviceStatus_blankDeviceId_throwsException() {
        repository.getDeviceStatus("   ");
    }

    // ========== getDeviceStatusLiveData tests ==========

    @Test
    public void getDeviceStatusLiveData_returnsNonNullLiveData() {
        LiveData<com.manuscripta.student.domain.model.DeviceStatus> liveData =
                repository.getDeviceStatusLiveData();

        assertNotNull(liveData);
    }

    @Test
    public void getDeviceStatusLiveData_initialValueIsNull() {
        LiveData<com.manuscripta.student.domain.model.DeviceStatus> liveData =
                repository.getDeviceStatusLiveData();

        assertNull(liveData.getValue());
    }

    // ========== updateStatus tests ==========

    @Test
    public void updateStatus_insertsEntityIntoDao() {
        repository.updateStatus(TEST_DEVICE_ID, DeviceStatus.ON_TASK, TEST_MATERIAL_ID, null);

        ArgumentCaptor<DeviceStatusEntity> captor =
                ArgumentCaptor.forClass(DeviceStatusEntity.class);
        verify(mockDao).insert(captor.capture());

        DeviceStatusEntity captured = captor.getValue();
        assertEquals(TEST_DEVICE_ID, captured.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, captured.getStatus());
        assertEquals(TEST_MATERIAL_ID, captured.getCurrentMaterialId());
    }

    @Test
    public void updateStatus_updatesLiveData() {
        repository.updateStatus(TEST_DEVICE_ID, DeviceStatus.ON_TASK, TEST_MATERIAL_ID, null);

        com.manuscripta.student.domain.model.DeviceStatus liveDataValue =
                repository.getDeviceStatusLiveData().getValue();

        assertNotNull(liveDataValue);
        assertEquals(TEST_DEVICE_ID, liveDataValue.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, liveDataValue.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateStatus_nullDeviceId_throwsException() {
        repository.updateStatus(null, DeviceStatus.ON_TASK, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateStatus_nullStatus_throwsException() {
        repository.updateStatus(TEST_DEVICE_ID, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateStatus_blankDeviceId_throwsException() {
        repository.updateStatus("   ", DeviceStatus.ON_TASK, null, null);
    }

    // ========== updateBatteryLevel tests ==========

    @Test
    public void updateBatteryLevel_validLevel_updatesBatteryLevel() {
        // First initialise with a device
        repository.updateStatus(TEST_DEVICE_ID, DeviceStatus.IDLE, null, null);

        DeviceStatusEntity entity = createTestEntity(TEST_DEVICE_ID, DeviceStatus.IDLE);
        when(mockDao.getById(TEST_DEVICE_ID)).thenReturn(entity);

        repository.updateBatteryLevel(TEST_BATTERY_LEVEL);

        assertEquals(TEST_BATTERY_LEVEL, repository.getCurrentBatteryLevel());

        // Verify DAO persistence (insert called twice: once for updateStatus, once for battery)
        verify(mockDao, times(2)).insert(any(DeviceStatusEntity.class));

        // Verify LiveData was updated with new battery level
        com.manuscripta.student.domain.model.DeviceStatus liveDataValue =
                repository.getDeviceStatusLiveData().getValue();
        assertNotNull(liveDataValue);
        assertEquals(TEST_BATTERY_LEVEL, liveDataValue.getBatteryLevel());
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateBatteryLevel_belowMinimum_throwsException() {
        repository.updateBatteryLevel(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateBatteryLevel_aboveMaximum_throwsException() {
        repository.updateBatteryLevel(101);
    }

    @Test
    public void updateBatteryLevel_atMinimum_succeeds() {
        repository.updateBatteryLevel(0);
        assertEquals(0, repository.getCurrentBatteryLevel());
    }

    @Test
    public void updateBatteryLevel_atMaximum_succeeds() {
        repository.updateBatteryLevel(100);
        assertEquals(100, repository.getCurrentBatteryLevel());
    }

    // ========== setOnTask tests ==========

    @Test
    public void setOnTask_updatesStatusToOnTask() {
        repository.setOnTask(TEST_DEVICE_ID, TEST_MATERIAL_ID);

        ArgumentCaptor<DeviceStatusEntity> captor =
                ArgumentCaptor.forClass(DeviceStatusEntity.class);
        verify(mockDao).insert(captor.capture());

        DeviceStatusEntity captured = captor.getValue();
        assertEquals(DeviceStatus.ON_TASK, captured.getStatus());
        assertEquals(TEST_MATERIAL_ID, captured.getCurrentMaterialId());
    }

    // ========== setIdle tests ==========

    @Test
    public void setIdle_updatesStatusToIdle() {
        repository.setIdle(TEST_DEVICE_ID);

        ArgumentCaptor<DeviceStatusEntity> captor =
                ArgumentCaptor.forClass(DeviceStatusEntity.class);
        verify(mockDao).insert(captor.capture());

        assertEquals(DeviceStatus.IDLE, captor.getValue().getStatus());
    }

    // ========== setLocked tests ==========

    @Test
    public void setLocked_updatesStatusToLocked() {
        repository.setLocked(TEST_DEVICE_ID);

        ArgumentCaptor<DeviceStatusEntity> captor =
                ArgumentCaptor.forClass(DeviceStatusEntity.class);
        verify(mockDao).insert(captor.capture());

        assertEquals(DeviceStatus.LOCKED, captor.getValue().getStatus());
    }

    // ========== setDisconnected tests ==========

    @Test
    public void setDisconnected_updatesStatusToDisconnected() {
        repository.setDisconnected(TEST_DEVICE_ID);

        ArgumentCaptor<DeviceStatusEntity> captor =
                ArgumentCaptor.forClass(DeviceStatusEntity.class);
        verify(mockDao).insert(captor.capture());

        assertEquals(DeviceStatus.DISCONNECTED, captor.getValue().getStatus());
    }

    // ========== clearDeviceStatus tests ==========

    @Test
    public void clearDeviceStatus_deletesFromDao() {
        repository.clearDeviceStatus(TEST_DEVICE_ID);

        verify(mockDao).deleteById(TEST_DEVICE_ID);
    }

    @Test
    public void clearDeviceStatus_currentDevice_clearsLiveData() {
        // First set the current device
        repository.updateStatus(TEST_DEVICE_ID, DeviceStatus.IDLE, null, null);
        assertNotNull(repository.getDeviceStatusLiveData().getValue());

        // Clear it
        repository.clearDeviceStatus(TEST_DEVICE_ID);

        assertNull(repository.getDeviceStatusLiveData().getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void clearDeviceStatus_nullDeviceId_throwsException() {
        repository.clearDeviceStatus(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void clearDeviceStatus_blankDeviceId_throwsException() {
        repository.clearDeviceStatus("   ");
    }

    @Test
    public void clearDeviceStatus_nonCurrentDevice_doesNotAffectLiveData() {
        // Set device-1 as current
        repository.updateStatus(TEST_DEVICE_ID, DeviceStatus.IDLE, null, null);
        assertNotNull(repository.getDeviceStatusLiveData().getValue());
        assertEquals(TEST_DEVICE_ID,
                repository.getDeviceStatusLiveData().getValue().getDeviceId());

        // Clear device-2 (not current)
        repository.clearDeviceStatus("other-device-456");

        // LiveData should still contain device-1's status
        assertNotNull(repository.getDeviceStatusLiveData().getValue());
        assertEquals(TEST_DEVICE_ID,
                repository.getDeviceStatusLiveData().getValue().getDeviceId());
    }

    // ========== clearAllDeviceStatus tests ==========

    @Test
    public void clearAllDeviceStatus_deletesAllFromDao() {
        repository.clearAllDeviceStatus();

        verify(mockDao).deleteAll();
    }

    @Test
    public void clearAllDeviceStatus_clearsLiveData() {
        // First set a device
        repository.updateStatus(TEST_DEVICE_ID, DeviceStatus.IDLE, null, null);

        repository.clearAllDeviceStatus();

        assertNull(repository.getDeviceStatusLiveData().getValue());
    }

    // ========== initialiseDeviceStatus tests ==========

    @Test
    public void initialiseDeviceStatus_newDevice_createsIdleStatus() {
        when(mockDao.getById(TEST_DEVICE_ID)).thenReturn(null);

        repository.initialiseDeviceStatus(TEST_DEVICE_ID);

        ArgumentCaptor<DeviceStatusEntity> captor =
                ArgumentCaptor.forClass(DeviceStatusEntity.class);
        verify(mockDao).insert(captor.capture());

        assertEquals(DeviceStatus.IDLE, captor.getValue().getStatus());
    }

    @Test
    public void initialiseDeviceStatus_existingDevice_loadsExistingStatus() {
        DeviceStatusEntity existing = createTestEntity(TEST_DEVICE_ID, DeviceStatus.ON_TASK);
        when(mockDao.getById(TEST_DEVICE_ID)).thenReturn(existing);

        repository.initialiseDeviceStatus(TEST_DEVICE_ID);

        // Should not insert new entity
        verify(mockDao, never()).insert(any());

        // Should load existing status into LiveData
        com.manuscripta.student.domain.model.DeviceStatus liveDataValue =
                repository.getDeviceStatusLiveData().getValue();
        assertNotNull(liveDataValue);
        assertEquals(DeviceStatus.ON_TASK, liveDataValue.getStatus());
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialiseDeviceStatus_nullDeviceId_throwsException() {
        repository.initialiseDeviceStatus(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void initialiseDeviceStatus_blankDeviceId_throwsException() {
        repository.initialiseDeviceStatus("   ");
    }

    // ========== getCurrentBatteryLevel tests ==========

    @Test
    public void getCurrentBatteryLevel_defaultValue_returns100() {
        assertEquals(100, repository.getCurrentBatteryLevel());
    }

    // ========== Thread safety tests ==========

    @Test
    public void concurrentUpdates_differentDevices_areThreadSafe() throws InterruptedException {
        int threadCount = 10;
        java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                repository.updateStatus(
                        "device-" + index,
                        DeviceStatus.ON_TASK,
                        null,
                        null
                );
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));

        // Verify all 10 inserts were called
        verify(mockDao, times(threadCount)).insert(any(DeviceStatusEntity.class));
    }

    @Test
    public void concurrentUpdates_sameDevice_areThreadSafe() throws InterruptedException {
        // Test concurrent updates to the SAME device ID to verify proper synchronization
        int threadCount = 10;
        java.util.concurrent.CountDownLatch latch =
                new java.util.concurrent.CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                // All threads update the same device with different statuses
                DeviceStatus status = (index % 2 == 0) ? DeviceStatus.ON_TASK : DeviceStatus.IDLE;
                repository.updateStatus(
                        TEST_DEVICE_ID,
                        status,
                        "material-" + index,
                        null
                );
                latch.countDown();
            }).start();
        }

        assertTrue(latch.await(5, java.util.concurrent.TimeUnit.SECONDS));

        // Verify all 10 inserts were called (all targeting same device)
        verify(mockDao, times(threadCount)).insert(any(DeviceStatusEntity.class));

        // Verify final state is consistent (LiveData has a value for TEST_DEVICE_ID)
        com.manuscripta.student.domain.model.DeviceStatus finalStatus =
                repository.getDeviceStatusLiveData().getValue();
        assertNotNull(finalStatus);
        assertEquals(TEST_DEVICE_ID, finalStatus.getDeviceId());
    }

    // ========== Helper methods ==========

    private DeviceStatusEntity createTestEntity(String deviceId, DeviceStatus status) {
        return new DeviceStatusEntity(
                deviceId,
                status,
                TEST_BATTERY_LEVEL,
                TEST_MATERIAL_ID,
                null,
                System.currentTimeMillis()
        );
    }
}
