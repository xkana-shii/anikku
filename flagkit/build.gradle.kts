plugins {
    id("mihon.library")
    kotlin("android")
}

android {
    namespace = "com.murgupluoglu.flagkit"
}

// AGP 8.13 can restore incomplete FlagKit resource outputs from the local build cache on Windows.
// The affected entries contain only 39 of the 254 source vectors, which subsequently removes
// country drawables from the consuming app's R class. Keep normal up-to-date checks, but do not
// reuse cache entries for the FlagKit resource/R pipeline until AGP correctly fingerprints it.
tasks.configureEach {
    if (name.matches(Regex("(generate|package|parse|compile|process).*(Resources|RFile)"))) {
        outputs.cacheIf("FlagKit resource cache outputs must contain every vector") { false }
    }
}

dependencies {
    implementation(projects.core.common)
}
