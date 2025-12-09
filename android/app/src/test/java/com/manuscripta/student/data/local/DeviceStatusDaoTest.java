package com.manuscripta.student.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.manuscripta.student.data.model.DeviceStatus;
import com.manuscripta.student.data.model.DeviceStatusEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.List;

/**
 * Unit tests for {@link DeviceStatusDao}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, manifest = Config.NONE)
public class DeviceStatusDaoTest {

    private ManuscriptaDatabase database;
    private DeviceStatusDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, ManuscriptaDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.deviceStatusDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void testInsertAndGetById() {
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, "mat-1", "view-1", System.currentTimeMillis());
        dao.insert(entity);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNotNull(retrieved);
        assertEquals("dev-1", retrieved.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, retrieved.getStatus());
        assertEquals(100, retrieved.getBatteryLevel());
        assertEquals("mat-1", retrieved.getCurrentMaterialId());
        assertEquals("view-1", retrieved.getStudentView());
    }

    @Test
    public void testInsertReplacement() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, "mat-1", "view-1", System.currentTimeMillis());
        dao.insert(entity1);

        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-1", DeviceStatus.HAND_RAISED, 90, "mat-2", "view-2", System.currentTimeMillis());
        dao.insert(entity2);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNotNull(retrieved);
        assertEquals(DeviceStatus.HAND_RAISED, retrieved.getStatus());
        assertEquals(90, retrieved.getBatteryLevel());
        assertEquals("mat-2", retrieved.getCurrentMaterialId());
        assertEquals("view-2", retrieved.getStudentView());
    }

    @Test
    public void testGetAll() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, null, null, System.currentTimeMillis());
        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-2", DeviceStatus.DISCONNECTED, 0, null, null, System.currentTimeMillis());
        dao.insert(entity1);
        dao.insert(entity2);

        List<DeviceStatusEntity> all = dao.getAll();
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, "mat-1", "view-1", System.currentTimeMillis());
        dao.insert(entity);

        // Create a new entity with updated values since entities are immutable
        DeviceStatusEntity updatedEntity = new DeviceStatusEntity(
                "dev-1", DeviceStatus.LOCKED, 50, "mat-updated", "view-updated", System.currentTimeMillis());
        dao.update(updatedEntity);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertEquals(DeviceStatus.LOCKED, retrieved.getStatus());
        assertEquals(50, retrieved.getBatteryLevel());
        assertEquals("mat-updated", retrieved.getCurrentMaterialId());
        assertEquals("view-updated", retrieved.getStudentView());
    }

    @Test
    public void testDeleteById() {
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, null, null, System.currentTimeMillis());
        dao.insert(entity);

        dao.deleteById("dev-1");

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNull(retrieved);
    }

    @Test
    public void testNewStatuses() {
        // Test IDLE
        DeviceStatusEntity idleEntity = new DeviceStatusEntity("dev-idle", DeviceStatus.IDLE, 70, null, null, System.currentTimeMillis());
        dao.insert(idleEntity);
        DeviceStatusEntity retrievedIdle = dao.getById("dev-idle");
        assertEquals(DeviceStatus.IDLE, retrievedIdle.getStatus());
    }

    @Test
    public void testInsertAll() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, null, null, System.currentTimeMillis());
        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-2", DeviceStatus.DISCONNECTED, 0, null, null, System.currentTimeMillis());
        List<DeviceStatusEntity> list = java.util.Arrays.asList(entity1, entity2);

        dao.insertAll(list);

        List<DeviceStatusEntity> all = dao.getAll();
        assertEquals(2, all.size());
    }

    @Test
    public void testDelete() {
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, null, null, System.currentTimeMillis());
        dao.insert(entity);

        dao.delete(entity);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNull(retrieved);
    }

    @Test
    public void testDeleteAll() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, null, null, System.currentTimeMillis());
        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-2", DeviceStatus.DISCONNECTED, 0, null, null, System.currentTimeMillis());
        dao.insert(entity1);
        dao.insert(entity2);

        dao.deleteAll();

        List<DeviceStatusEntity> all = dao.getAll();
        assertEquals(0, all.size());
    }

    @Test
    public void testGetCount() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100, null, null, System.currentTimeMillis());
        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-2", DeviceStatus.DISCONNECTED, 0, null, null, System.currentTimeMillis());
        dao.insert(entity1);
        dao.insert(entity2);

        int count = dao.getCount();
        assertEquals(2, count);
    }
}
