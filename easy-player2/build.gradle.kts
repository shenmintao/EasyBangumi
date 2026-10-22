plugins {
    id("com.android.library")
    alias(build.plugins.kotlin.android)
    alias(build.plugins.compose.compiler)
    id("maven-publish")
}

android {
    namespace = "loli.ball.easyplayer2"
    compileSdk = com.heyanle.buildsrc.Android.compileSdk

    defaultConfig {
        // media3 1.9 自身要求 minSdk 23，与 app 的最低版本保持一致
        minSdk = com.heyanle.buildsrc.Android.minSdk
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs = listOf("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        // Robolectric needs the merged manifest/resources to host Compose UI tests.
        unitTests.isIncludeAndroidResources = true
    }
}

afterEvaluate {
    publishing {
        publications {
            create("maven_public", MavenPublication::class) {
                groupId = "loli.ball"
                artifactId = "easyplayer2"
                version = "1.0.0.test"
                from(components.getByName("release"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
    kotlinOptions.freeCompilerArgs += "-Xcontext-receivers"
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect:${build.versions.kotlin.get()}")

    val composeMaterialVersion = "1.6.8"
    implementation("androidx.compose.material:material:$composeMaterialVersion")
    implementation("androidx.compose.material:material-icons-core:$composeMaterialVersion")
    implementation("androidx.compose.material:material-icons-extended:$composeMaterialVersion")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.material3:material3:1.2.1")

    val media3 = "1.9.2"
    api("androidx.media3:media3-exoplayer:$media3")
    api("androidx.media3:media3-exoplayer-dash:$media3")
    api("androidx.media3:media3-ui:$media3")
    api("androidx.media3:media3-exoplayer-hls:$media3")
    api("androidx.media3:media3-transformer:$media3")
    api("androidx.media3:media3-common:$media3")
    api("androidx.media3:media3-effect:$media3")
    api("androidx.media3:media3-muxer:$media3")

    testImplementation("junit:junit:4.13.2")
    // 遥控器按键与焦点行为的 JVM UI 测试
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.7.4")
    testImplementation("androidx.compose.ui:ui-test-manifest:1.7.4")
}
