plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

val libGroupId = "com.sd.lib.android"
val libArtifactId = "streamx"
val libVersionName = "1.0.0-rc03"

android {
    namespace = "com.sd.lib.stream"
    compileSdk = 33
    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
        kotlinOptions.freeCompilerArgs += listOf("-module-name", "$libGroupId.$libArtifactId")
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = libGroupId
                artifactId = libArtifactId
                version = libVersionName
            }
        }
    }
}