plugins {
    java
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

// Student and reference implementations share one contract and one test suite.
sourceSets {
    main {
        java.setSrcDirs(listOf(
            if (providers.gradleProperty("reference").isPresent) "src/reference/java"
            else "src/main/java"
        ))
        // Gateway is common to both implementations.
        java.srcDir("src/common/java")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
}

tasks.test {
    useJUnitPlatform()
}
