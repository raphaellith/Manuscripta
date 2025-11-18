plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    id("checkstyle")
    id("jacoco")
}

android {
    namespace = "com.manuscripta.student"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.manuscripta.student"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true 
        }
    }

    buildFeatures {
        viewBinding = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // AndroidX
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)

    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("com.google.dagger:hilt-android-testing:2.52")
    kspTest("com.google.dagger:hilt-android-compiler:2.52")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Checkstyle configuration
checkstyle {
    toolVersion = "10.12.0"
    configFile = file("${project.rootDir}/config/checkstyle/checkstyle.xml")
}

tasks.register<Checkstyle>("checkstyle") {
    source = fileTree("src/main/java")
    include("**/*.java")
    classpath = files()
}

// --- JACOCO CONFIGURATION (JAVA FOCUSED) ---

jacoco {
    toolVersion = "0.8.12"
}

// Consolidated list of exclusions for both Report and Verification
val jacocoExclusions = listOf(
    // Android Framework
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "android/**/*.*",
    
    // Data Binding / View Binding
    "**/databinding/*",
    "**/BR.class",
    "**/*Binding.class",

    // Hilt & Dagger (The generated Java noise)
    "**/Hilt_*.class",
    "**/dagger/hilt/internal/**",
    "**/hilt_aggregated_deps/**", // Explicitly removes the package from your screenshot
    "**/*_Factory.class",
    "**/*_MembersInjector.class",
    "**/*_HiltModules*.class",
    "**/Dagger*Component.class",
    "**/Dagger*Component\$Builder.class",
    "**/*Module_*Factory.class",
    
    // Room Generated Code
    "**/*_Impl.class",
    "**/*_Impl\$*.class"
)

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
    }

    // For Java projects, the classes are in intermediates/javac/debug/classes
    val debugTree = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/classes").exclude(jacocoExclusions)
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    
    // Point to the execution data
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    val debugTree = fileTree("${layout.buildDirectory.get()}/intermediates/javac/debug/classes").exclude(jacocoExclusions)
    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(layout.buildDirectory.get()) {
        include("jacoco/testDebugUnitTest.exec")
    })

    violationRules {
        rule {
            // Enforce 100% coverage on Instructions
            limit {
                counter = "INSTRUCTION"
                value = "COVEREDRATIO"
                minimum = "1.0".toBigDecimal() // 100%
            }
        }
    }
}
