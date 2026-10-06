plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.experimentos.mobile"
    compileSdk = 36

    val apiBaseUrl = providers.gradleProperty("apiBaseUrl")
        .orElse("https://safespace-backend-q3uv.onrender.com/")
        .get()

    defaultConfig {
        applicationId = "com.experimentos.mobile"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        debug {
            manifestPlaceholders["allowCleartext"] = "true"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["allowCleartext"] = "false"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.01")

    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.2.0-alpha01")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.9.6")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.1.0")
    testImplementation("org.robolectric:robolectric:4.16.1")

    constraints {
        testImplementation("org.bouncycastle:bcprov-jdk18on:1.85.2") {
            version { strictly("1.85.2") }
            because("Robolectric's 1.81 provider has four OSV advisories; the patched 1.85 maintenance release is test-only.")
        }
    }
}

// Read-only resolution used by the repeatable dependency audit; never ships in the APK.
tasks.register("validationDependencyInventory") {
    doLast {
        val scopes = listOf("debugRuntimeClasspath", "debugUnitTestRuntimeClasspath")
        val json = scopes.joinToString(",", prefix = "{", postfix = "}") { scope ->
            val coordinates = configurations.getByName(scope).resolvedConfiguration.resolvedArtifacts
                .map { it.moduleVersion.id }
                .map { "${it.group}:${it.name}:${it.version}" }
                .distinct().sorted()
            "\"$scope\":" + coordinates.joinToString(",", prefix = "[", postfix = "]") { "\"$it\"" }
        }
        val output = layout.buildDirectory.file("reports/validation-dependencies.json").get().asFile
        output.parentFile.mkdirs()
        output.writeText(json)
    }
}
