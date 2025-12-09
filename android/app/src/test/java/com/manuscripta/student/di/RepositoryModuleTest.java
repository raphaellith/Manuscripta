package com.manuscripta.student.di;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.manuscripta.student.data.local.ManuscriptaDatabase;
import com.manuscripta.student.data.local.SessionDao;
import com.manuscripta.student.data.repository.SessionRepository;
import com.manuscripta.student.data.repository.SessionRepositoryImpl;

import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link RepositoryModule}.
 */
public class RepositoryModuleTest {

    private RepositoryModule repositoryModule;
    private ManuscriptaDatabase mockDatabase;
    private SessionDao mockSessionDao;

    @Before
    public void setUp() {
        repositoryModule = new RepositoryModule();
        mockDatabase = mock(ManuscriptaDatabase.class);
        mockSessionDao = mock(SessionDao.class);
    }

    @Test
    public void testProvideSessionDao_returnsDao() {
        when(mockDatabase.sessionDao()).thenReturn(mockSessionDao);

        SessionDao result = repositoryModule.provideSessionDao(mockDatabase);

        assertNotNull(result);
        verify(mockDatabase).sessionDao();
    }

    @Test
    public void testProvideSessionRepository_returnsRepository() {
        SessionRepository result = repositoryModule.provideSessionRepository(mockSessionDao);

        assertNotNull(result);
        assertTrue(result instanceof SessionRepositoryImpl);
    }
}
