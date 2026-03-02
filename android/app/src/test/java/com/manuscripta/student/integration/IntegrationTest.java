package com.manuscripta.student.integration;

/**
 * Marker interface for JUnit {@code @Category} filtering.
 *
 * <p>All integration tests that require a running Windows server are annotated
 * with {@code @Category(IntegrationTest.class)} so they can be excluded from
 * the default {@code ./gradlew test} run and included explicitly with
 * {@code ./gradlew test -Pintegration}.</p>
 */
public interface IntegrationTest {
}
