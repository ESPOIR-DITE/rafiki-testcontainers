import org.jreleaser.model.Active

plugins {
    java
    `maven-publish`
    id("org.jreleaser") version "1.14.0"
    id("io.github.sgtsilvio.gradle.metadata") version "0.6.0"
}

group = "zm.hashcode"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    implementation(platform("org.testcontainers:testcontainers-bom:2.0.3"))
    implementation("org.testcontainers:testcontainers")

    testImplementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation("org.awaitility:awaitility:4.2.0")

    implementation("org.apache.commons:commons-compress:1.26.0")
    implementation("commons-codec:commons-codec:1.16.0")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

metadata {
    readableName = "Rafiki Testcontainers"
    description = "Testcontainers wrapper for Rafiki - Interledger open payments backend for integration testing"
    license { apache2() }
    developers {
        register("boniface") {
            fullName.set("Boniface Kabaso")
            email.set("550236+boniface@users.noreply.github.com")
        }
    }
    github {
        org.set("hashcode-zm")
        repo.set("rafiki-testcontainers")
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "zm.hashcode"
            artifactId = "rafiki-testcontainers"
            version = "0.1.0"

            from(components["java"])

            pom {
                name.set("Rafiki Testcontainers")
                description.set("Testcontainers wrapper for Rafiki - Interledger open payments backend for integration testing")
                url.set("https://github.com/hashcode-zm/rafiki-testcontainers")
                inceptionYear.set("2025")

                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }

                developers {
                    developer {
                        id.set("boniface")
                        name.set("Boniface Kabaso")
                        email.set("550236+boniface@users.noreply.github.com")
                    }
                }

                scm {
                    url.set("https://github.com/hashcode-zm/rafiki-testcontainers")
                    connection.set("scm:git:https://github.com/hashcode-zm/rafiki-testcontainers.git")
                    developerConnection.set("scm:git:ssh://git@github.com/hashcode-zm/rafiki-testcontainers.git")
                }
            }
        }
    }

    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

jreleaser {
    project {
        description.set("Testcontainers wrapper for Rafiki - Interledger open payments backend for integration testing")
        authors.set(listOf("Boniface Kabaso"))
        license.set("Apache-2.0")
        links {
            homepage.set("https://github.com/hashcode-zm/rafiki-testcontainers")
        }
        inceptionYear.set("2025")
        copyright.set("2025 Boniface Kabaso")
    }

    release {
        github {
            repoOwner.set("hashcode-zm")
            name.set("rafiki-testcontainers")
            tagName.set("{{projectVersion}}")
            releaseName.set("Release {{projectVersion}}")
            overwrite.set(false)
            update {
                enabled.set(true)
            }
            changelog {
                enabled.set(true)
                formatted.set(Active.ALWAYS)
                preset.set("conventional-commits")
                contributors {
                    enabled.set(true)
                }
                append {
                    enabled.set(true)
                    target.set(file("CHANGELOG.md"))
                    content.set("""
                        ## [{{projectVersion}}] - {{releaseDate}}
                        {{changelogChanges}}
                        {{changelogContributors}}
                    """.trimIndent())
                }
            }
        }
    }

    signing {
        active = Active.ALWAYS
        armored = true
    }

    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active = Active.ALWAYS
                    url = "https://central.sonatype.com/api/v1/publisher"
                    stagingRepository(layout.buildDirectory.dir("staging-deploy").get().toString())
                }
            }
        }
    }
}
