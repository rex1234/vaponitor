plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.detekt)
}

group = "life.vaporized"
version = "0.9"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

tasks {
    val shadowJar by getting(com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class) {
        archiveBaseName.set("ktor-app")
        archiveClassifier.set("")
        archiveVersion.set("")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "io.ktor.server.netty.EngineMain"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.kotlinx.datetime)

    // Ktor Server (using bundle)
    implementation(libs.bundles.ktor.server)

    // Ktor Client (required by Discord/Kord library)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.logback.classic)

    // OkHttp
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.ktor)

    // Exposed (using bundle)
    implementation(libs.bundles.exposed)
    implementation(libs.sqlite.jdbc)

    // Other
    implementation(libs.snakeyaml)
    implementation(libs.dotenv.kotlin)

    implementation(libs.kord.core)
    implementation(libs.oshi.core)

    detektPlugins(libs.detekt.formatting)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

detekt {
    buildUponDefaultConfig = true // Use default rules
    allRules = false // Disable all rules (override per file)
    config.setFrom(files("$rootDir/detekt-config.yml")) // Custom configuration file (optional)
    baseline = file("$rootDir/detekt-baseline.xml") // Ignore existing issues file (optional)
}
