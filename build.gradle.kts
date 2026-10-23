import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "11.0.12"
  kotlin("jvm") version "2.4.21"
  kotlin("plugin.spring") version "2.4.21"
  kotlin("plugin.jpa") version "2.4.21"
  id("jacoco")
  id("org.barfuin.gradle.jacocolog") version "4.0.3"
  id("maven-publish")
  id("signing")
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
  kotlin("plugin.serialization") version "2.4.21"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val awsSdkVersion = "2.57.0"
val testContainersVersion = "1.21.4"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:3.0.3")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:7.4.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.12.0")
  implementation("org.springframework.boot:spring-boot-starter-webclient")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("com.google.code.gson:gson:2.14.0")
  implementation("com.google.guava:guava:33.7.2-jre")
  implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
  implementation("com.cronutils:cron-utils:9.2.1")

  // AWS
  implementation("software.amazon.awssdk:redshiftdata:$awsSdkVersion")
  implementation("software.amazon.awssdk:s3:$awsSdkVersion")
  implementation("software.amazon.awssdk:athena:$awsSdkVersion")
  implementation("software.amazon.awssdk:sts:$awsSdkVersion")
  implementation("software.amazon.awssdk:dynamodb:$awsSdkVersion")

  // Swagger
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")

  // Excel (xlsx) report downloads. Kept out of the public API surface, so `implementation`
  // is enough - consumers get it transitively at runtime.
  implementation("org.apache.poi:poi-ooxml:5.5.1")

  // Postgres dependencies
  implementation("org.flywaydb:flyway-core")
  runtimeOnly("org.flywaydb:flyway-database-postgresql")
  implementation("org.postgresql:postgresql:42.7.14")

  // Testing
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.12.0")
  testImplementation("org.awaitility:awaitility-kotlin:4.3.0")
  testImplementation("org.testcontainers:localstack:1.21.4")
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:3.0.3")
  testImplementation("com.h2database:h2")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  testImplementation("io.jsonwebtoken:jjwt:0.13.0")
  testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
  testImplementation("org.postgresql:postgresql:42.7.14")
  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")
  testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
  testImplementation("org.springframework.boot:spring-boot-starter-webflux-test") // webclient-test does not autoconfig a webtestclient, so we need to use webflux-test

  // Fix for security issue in transient dependency
  implementation("ch.qos.logback:logback-classic:1.6.5")
  implementation("ch.qos.logback:logback-core:1.6.5")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
  jvmToolchain(21)
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}

// Safely wire the dependency whenever 'sourcesJar' gets created
tasks.matching { it.name == "sourcesJar" }.configureEach {
  dependsOn(tasks.named("generateGitProperties"))
}

publishing {
  repositories {
    mavenLocal()
    mavenCentral()
  }
  publications {
    create<MavenPublication>("digitalprisonreportinglib") {
      from(components["java"])
      pom {
        group = "uk.gov.justice.service.hmpps"
        name.set(base.archivesName)
        artifactId = base.archivesName.get()
        version = project.findProperty("publishVersion") as String?
        description.set("A Spring Boot reporting library to be integrated into your project and allow you to produce reports.")
        url.set("https://github.com/ministryofjustice/hmpps-digital-prison-reporting-lib")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
        developers {
          developer {
            id.set("gavriil-g-moj")
            name.set("Digital Prison Reporting")
            email.set("digitalprisonreporting@digital.justice.gov.uk")
          }
        }
        scm {
          url.set("https://github.com/ministryofjustice/hmpps-digital-prison-reporting-mi-lib")
        }
      }
    }
  }
}
signing {
  setRequired {
    gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
  }
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(publishing.publications["digitalprisonreportinglib"])
}
java.sourceCompatibility = JavaVersion.VERSION_21

tasks.bootJar {
  enabled = false
}

tasks.jar {
  enabled = true
}

repositories {
  mavenLocal()
  mavenCentral()
}

java {
  withSourcesJar()
  withJavadocJar()
}

fun isNonStable(version: String): Boolean {
  val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
  val regex = "^[0-9,.v-]+(-r)?$".toRegex()
  val isStable = stableKeyword || regex.matches(version)
  return isStable.not()
}

tasks {
  withType<Test> {
    useJUnitPlatform()
  }

  withType<DependencyUpdatesTask> {
    rejectVersionIf {
      isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
  }
}

project.getTasksByName("check", false).forEach {
  val prefix = if (it.path.contains(":")) {
    it.path.substringBeforeLast(":")
  } else {
    ""
  }
  it.dependsOn("$prefix:ktlintCheck")
}
nexusPublishing {
  repositories {
    create("sonatype") {
      nexusUrl.set(uri("https://ossrh-staging-api.central.sonatype.com/service/local/"))
      snapshotRepositoryUrl.set(uri("https://central.sonatype.com/repository/maven-snapshots/"))
      username.set(System.getenv("OSSRH_USERNAME"))
      password.set(System.getenv("OSSRH_PASSWORD"))
    }
  }
}
