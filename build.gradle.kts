import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.+"
  kotlin("jvm") version "1.9.10"
  kotlin("plugin.spring") version "1.9.10"
  kotlin("plugin.jpa") version "1.9.10"
  id("jacoco")
  id("org.barfuin.gradle.jacocolog") version "3.1.0"
  id("maven-publish")
  id("signing")
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
//  implementation("com.amazon.redshift:redshift-jdbc4-no-awssdk:1.2.45.1069")

  implementation("com.google.code.gson:gson:2.8.5")

  // Security
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  // Swagger
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

  // Testing
  testImplementation("com.h2database:h2")
  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("javax.xml.bind:jaxb-api:2.4.0-b180830.0359")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }
}

// repositories {
//  mavenCentral()
//  maven("https://s3.amazonaws.com/redshift-maven-repository/release")
// }

kotlin {
  jvmToolchain(19)
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
  }
  publications {
    create<MavenPublication>("digitalprisonreportingmilib") {
      from(components["java"])
      pom {
        name.set(base.archivesName)
        artifactId = base.archivesName.get()
        description.set("A Spring Boot reporting library to be integrated into your project and allow you to produce reports.")
        url.set("https://github.com/ministryofjustice/hmpps-digital-prison-reporting-mi")
        licenses {
          license {
            name.set("MIT")
            url.set("https://opensource.org/licenses/MIT")
          }
        }
//        developers {
//          developer {
//            id.set("gavriil-g-moj")
//            name.set("Gavriil  Gavriilidis")
//            email.set("gavriil.gavriilidis@digital.justice.gov.uk")
//          }
//        }
        scm {
          url.set("https://github.com/ministryofjustice/hmpps-digital-prison-reporting-mi")
        }
      }
    }
  }
}
signing {
  val signingKey: String? by project
  val signingPassword: String? by project
  useInMemoryPgpKeys(signingKey, signingPassword)
  sign(publishing.publications["digitalprisonreportingmilib"])
}
java.sourceCompatibility = JavaVersion.VERSION_19

tasks.bootJar {
  enabled = false
}

tasks.jar {
  enabled = true
}

// repositories {
//  mavenLocal()
//  mavenCentral()
// }

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
  withType<KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
    }
  }

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
