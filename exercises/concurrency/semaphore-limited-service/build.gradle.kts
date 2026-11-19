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

// Both source sets exist regardless of Gradle properties; IntelliJ can
// index the student solution and the reference implementation together.
sourceSets {
    main {
        java.srcDirs("src/main/java", "src/common/java")
    }
    create("reference") {
        java.srcDirs("src/reference/java", "src/common/java")
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
}

tasks.test {
    useJUnitPlatform()

    // Preserve the existing documented -Preference=true command.
    // Compile the tests against the student-facing public contract,
    // but run those same tests against the separately compiled reference.
    if (providers.gradleProperty("reference").map(String::toBoolean).getOrElse(false)) {
        val reference = sourceSets["reference"]
        dependsOn(tasks.named(reference.classesTaskName))
        classpath = sourceSets["test"].output + reference.output + configurations["testRuntimeClasspath"]
    }
}
