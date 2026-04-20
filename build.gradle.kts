plugins {
    java
    eclipse
    application
    id("org.javamodularity.moduleplugin") version "1.8.15"
    id("org.openjfx.javafxplugin") version "0.0.13"
    id("org.beryx.jlink") version "2.25.0"
}

group = "com.uv.naloge"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val junitVersion = "5.12.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

application {
    mainModule.set("com.uv.naloge.tretjaNaloga")
    mainClass.set("com.uv.naloge.naloga3.ReservationApplication")
}

javafx {
    version = "21.0.6"
    modules = listOf("javafx.controls", "javafx.fxml")
}

dependencies {
    implementation("org.kordamp.bootstrapfx:bootstrapfx-core:0.4.0")
    implementation("org.kordamp.ikonli:ikonli-javafx:12.3.1")
    implementation("org.kordamp.ikonli:ikonli-bootstrapicons-pack:12.3.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jlink {
    imageZip.set(layout.buildDirectory.file("/distributions/app-${javafx.platform.classifier}.zip"))
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    launcher {
        name = "app"
    }
}

eclipse {
    classpath {
        file {
            whenMerged {
                val cp = this as org.gradle.plugins.ide.eclipse.model.Classpath
                cp.entries.forEach { entry ->
                    if (entry is org.gradle.plugins.ide.eclipse.model.Library) {
                        entry.entryAttributes.put("module", "true")
                    }
                }
            }
        }
    }
}
