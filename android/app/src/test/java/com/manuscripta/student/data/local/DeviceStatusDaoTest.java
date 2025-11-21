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
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100);
        dao.insert(entity);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNotNull(retrieved);
        assertEquals("dev-1", retrieved.getDeviceId());
        assertEquals(DeviceStatus.ON_TASK, retrieved.getStatus());
        assertEquals(100, retrieved.getBatteryLevel());
    }

    @Test
    public void testInsertReplacement() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100);
        dao.insert(entity1);

        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-1", DeviceStatus.NEEDS_HELP, 90);
        dao.insert(entity2);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNotNull(retrieved);
        assertEquals(DeviceStatus.NEEDS_HELP, retrieved.getStatus());
        assertEquals(90, retrieved.getBatteryLevel());
    }

    @Test
    public void testGetAll() {
        DeviceStatusEntity entity1 = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100);
        DeviceStatusEntity entity2 = new DeviceStatusEntity("dev-2", DeviceStatus.DISCONNECTED, 0);
        dao.insert(entity1);
        dao.insert(entity2);

        List<DeviceStatusEntity> all = dao.getAll();
        assertEquals(2, all.size());
    }

    @Test
    public void testUpdate() {
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100);
        dao.insert(entity);

        entity.setStatus(DeviceStatus.LOCKED);
        entity.setBatteryLevel(50);
        dao.update(entity);

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertEquals(DeviceStatus.LOCKED, retrieved.getStatus());
        assertEquals(50, retrieved.getBatteryLevel());
    }

    @Test
    public void testDeleteById() {
        DeviceStatusEntity entity = new DeviceStatusEntity("dev-1", DeviceStatus.ON_TASK, 100);
        dao.insert(entity);

        dao.deleteById("dev-1");

        DeviceStatusEntity retrieved = dao.getById("dev-1");
        assertNull(retrieved);
    }
    
    @Test
    public void testNewStatuses() {
        // Test LOCKED
        DeviceStatusEntity lockedEntity = new DeviceStatusEntity("dev-locked", DeviceStatus.LOCKED, 80);
        dao.insert(lockedEntity);
        DeviceStatusEntity retrievedLocked = dao.getById("dev-locked");
        assertEquals(DeviceStatus.LOCKED, retrievedLocked.getStatus());
        
        // Test IDLE
        DeviceStatusEntity idleEntity = new DeviceStatusEntity("dev-idle", DeviceStatus.IDLE, 70);
        dao.insert(idleEntity);
        DeviceStatusEntity retrievedIdle = dao.getById("dev-idle");
        assertEquals(DeviceStatus.IDLE, retrievedIdle.getStatus());
    }
}
