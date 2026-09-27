import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "com.opensdk.analytics"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Zero third-party runtime dependencies — uses only the Kotlin stdlib, the
    // Android framework, and org.json (bundled).
    testImplementation("junit:junit:4.13.2")
}

// ── Maven Central (Sonatype Central Portal) publishing ──────────────────────
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = true)
    signAllPublications()

    coordinates("io.github.mr-perfect-252", "apex-analytics", "1.0.0")

    configure(
        com.vanniktech.maven.publish.AndroidSingleVariantLibrary(
            variant = "release",
            sourcesJar = true,
            publishJavadocJar = true,
        )
    )

    pom {
        name.set("Apex Analytics — Android")
        description.set("ApexHub's Android analytics SDK — sessions, screen views, offline batching and user-submitted crash reports. Points at the ApexHub backend.")
        url.set("https://github.com/Mr-Perfect-252/open-analytics-android")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("Mr-Perfect-252")
                name.set("Sohan Ananthula")
                url.set("https://github.com/Mr-Perfect-252")
            }
        }
        scm {
            url.set("https://github.com/Mr-Perfect-252/open-analytics-android")
            connection.set("scm:git:git://github.com/Mr-Perfect-252/open-analytics-android.git")
            developerConnection.set("scm:git:ssh://git@github.com/Mr-Perfect-252/open-analytics-android.git")
        }
    }
}
