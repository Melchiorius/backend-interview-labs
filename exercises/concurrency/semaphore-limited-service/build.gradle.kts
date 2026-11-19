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

// Each directory belongs to exactly one source set. IntelliJ cannot reliably
// model a source directory shared between main and reference.
sourceSets {
    val common by creating {
        java.srcDir("src/common/java")
    }

    main {
        java.setSrcDirs(listOf("src/main/java"))
        compileClasspath += common.output
        runtimeClasspath += common.output
    }

    create("reference") {
        java.srcDir("src/reference/java")
        compileClasspath += common.output
        runtimeClasspath += common.output
    }

    test {
        compileClasspath += common.output
        runtimeClasspath += common.output
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
}

tasks.test {
    useJUnitPlatform()

    // Select the reference at test runtime without replacing the source roots.
    if (providers.gradleProperty("reference").map(String::toBoolean).getOrElse(false)) {
        val reference = sourceSets["reference"]
        val common = sourceSets["common"]
        dependsOn(tasks.named(reference.classesTaskName))
        classpath = sourceSets["test"].output +
            reference.output +
            common.output +
            configurations["testRuntimeClasspath"]
    }
}
