import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.1.4"
  kotlin("jvm") version "2.1.21"
  kotlin("plugin.spring") version "2.1.21"
  kotlin("plugin.jpa") version "2.1.21"
  id("jacoco")
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
  id("maven-publish")
  id("signing")
  id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

val awsSdkVersion = "2.31.5"
val testContainersVersion = "1.20.6"

dependencies {
  implementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter:1.4.0")
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")

  implementation("com.google.code.gson:gson:2.12.1")
  implementation("com.google.guava:guava:33.4.8-jre")

  // AWS
  implementation("software.amazon.awssdk:redshiftdata:$awsSdkVersion")
  implementation("software.amazon.awssdk:athena:$awsSdkVersion")
  implementation("software.amazon.awssdk:sts:$awsSdkVersion")
  implementation("software.amazon.awssdk:dynamodb:$awsSdkVersion")

  // Swagger
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")

  // Testing
  testImplementation("uk.gov.justice.service.hmpps:hmpps-kotlin-spring-boot-starter-test:1.4.0")
  testImplementation("com.h2database:h2")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
  testImplementation("io.jsonwebtoken:jjwt:0.12.6")
  testImplementation("com.marcinziolo:kotlin-wiremock:2.1.1")
  testImplementation("org.postgresql:postgresql:42.7.5")
  testImplementation("org.testcontainers:postgresql:$testContainersVersion")
  testImplementation("org.testcontainers:junit-jupiter:$testContainersVersion")

  // Fix for security issue in transient dependency
  implementation("ch.qos.logback:logback-classic:1.5.18")
  implementation("ch.qos.logback:logback-core:1.5.18")
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
