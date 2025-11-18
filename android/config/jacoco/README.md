# JaCoCo Configuration

This directory contains configuration files for JaCoCo code coverage tool.

## Coverage Requirements

- **Target Coverage**: 100%
- **Excluded from Coverage**:
  - Generated code (R.class, BuildConfig, etc.)
  - Data binding classes
  - Hilt generated files (_Factory, _MembersInjector, etc.)
  - Dagger modules and components
  - Application class (ManuscriptaApplication)
  - Database (ManuscriptaDatabase)
  - Test files

## Running Coverage Reports

```bash
./gradlew jacocoTestReport
./gradlew jacocoTestCoverageVerification
```

The HTML report will be available at:
`app/build/reports/jacoco/jacocoTestReport/html/index.html`
