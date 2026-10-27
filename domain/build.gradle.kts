plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Keep the domain Android-free and compatible with the app's existing Java target.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) } }

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
