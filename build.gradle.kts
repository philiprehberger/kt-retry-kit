plugins {
    kotlin("jvm") version "2.0.21"
    `maven-publish`
    signing
}

group = "com.philiprehberger"
version = project.findProperty("version") as String? ?: "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name.set("retry-kit")
                description.set("Coroutine-native retry with configurable backoff strategies for Kotlin")
                url.set("https://github.com/philiprehberger/kt-retry-kit")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("philiprehberger")
                        name.set("Philip Rehberger")
                    }
                }
                scm {
                    url.set("https://github.com/philiprehberger/kt-retry-kit")
                    connection.set("scm:git:git://github.com/philiprehberger/kt-retry-kit.git")
                    developerConnection.set("scm:git:ssh://github.com/philiprehberger/kt-retry-kit.git")
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/philiprehberger/kt-retry-kit/issues")
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME")
                password = System.getenv("OSSRH_PASSWORD")
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        System.getenv("GPG_PRIVATE_KEY"),
        System.getenv("GPG_PASSPHRASE")
    )
    sign(publishing.publications["maven"])
}
