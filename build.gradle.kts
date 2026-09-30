plugins {
    kotlin("multiplatform") version "1.9.24" apply false
    kotlin("plugin.serialization") version "1.9.24" apply false
    id("com.android.application") version "8.5.2" apply false
    id("com.android.library") version "8.5.2" apply false
    id("org.jetbrains.compose") version "1.5.12" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
