# hmpps-digital-prison-reporting-mi-lib
Common library to create reports.

[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-digital-prison-reporting-lib)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-digital-prison-reporting-lib "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-digital-prison-reporting-lib/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-digital-prison-reporting-lib)
[![Configured Data API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://digital-prison-reporting-mi-test.hmpps.service.justice.gov.uk/swagger-ui/index.html#/Configured%20Data%20API)
[![Report Definition API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://digital-prison-reporting-mi-test.hmpps.service.justice.gov.uk/swagger-ui/index.html#/Report%20Definition%20API)

This project is generated from ministryofjustice/hmpps-template-kotlin

Requires Java 19 or above

#### CODEOWNER

- Team : [hmpps-digital-prison-reporting](https://github.com/orgs/ministryofjustice/teams/hmpps-digital-prison-reporting)
- Email : digitalprisonreporting@digital.justice.gov.uk

## Overview
This is a library which you can include into your project to create reports.
It exposes three endpoints which are fully documented with OpenAPI documentation. 

## Local Development

This project uses gradle which is bundled with the repository and also makes use
of

- [spring boot](https://spring.io/projects/spring-boot) - as a web framework and for compile time dependency injection
- [jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html) - for test coverage reports

## Testing

> **Note** - test coverage reports are enabled by default and after running the
> tests the report will be written to build/reports/jacoco/test/html

### Unit Tests

The unit tests use JUnit5 and Mockito where appropriate. Use the following to
run the tests.

```
    ./gradlew clean test
```

### Integration Tests

```
    TBD
```

### Acceptance Tests

```
    TBD
```

## Contributing

Please adhere to the following guidelines when making contributions to the
project.

### Documentation

- Keep all code commentary and documentation up to date

### Branch Naming

- Use a JIRA ticket number where available
- Otherwise a short descriptive name is acceptable

### Commit Messages

- Prefix any commit messages with the JIRA ticket number where available
- Otherwise, use the prefix `NOJIRA`

### Pull Requests

- Reference or link any relevant JIRA tickets in the pull request notes
- At least one approval is required before a PR can be merged

### Versioning
* Use [semantic versioning](https://semver.org/) to indicate the scope of the change.
* For versions considered unstable add a suffix to the version number such as `-beta` or `-wip` to indicate this.

## Integrating to your Spring Boot project
To integrate the library into your project you will need to add the dependency to your build.gradle file, e.g:
`implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:1.0.0")`

You will also need to add the following to your Spring Boot application class:
`@ComponentScan("yourapplicationpackage","uk.gov.justice.digital.hmpps.digitalprisonreportinglib")`
Where you will need to replace "yourapplicationpackage" with the actual package of your application.

### Open API Docs
The API documentation is generated via the following dependency:
`implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")`
By using this for your Open API configuration you will be able to see the API docs on the swagger endpoint exposed by your application.

### Publishing Locally
Run the following command to publish the artifact to your local maven repository:
`./gradlew publishToMavenLocal -x signDigitalprisonreportinglibPublication`

## Publishing to Maven Central

Publishing to Maven central takes place through the Circle CI build pipeline when there are changes to the `main` branch.
The pipeline has a step that requires manual approval to perform the publication.

## Technical Details of Publishing to Maven Central

[This guide](https://central.sonatype.org/publish/publish-guide/) was used as a basis for publishing to Maven Central.

However, please note that the document above is old and a couple of things have changed.

* The Gradle plugin used in that document - `maven` - is out of date and the [maven-publish plugin](https://docs.gradle.org/current/userguide/publishing_maven.html) is used instead.
* The process described in the document above requires a manual step to release the library from the Nexus staging repository - we have implemented the  [Nexus Publish Plugin](https://github.com/gradle-nexus/publish-plugin) to automate this step.

### Authenticating with Sonatype

When publishing to Maven Central we authenticate with a username and password.

The groupId (see [Maven coordinates](https://maven.apache.org/pom.html#Maven_Coordinates)) of the project is `uk.org.justice.service.hmpps`.

We created a team user in [Sonatype Jira](https://issues.sonatype.org/) and our team user was added to the group `uk.org.justice.service.hmpps` (see [this Jira ticket that was raised](https://issues.sonatype.org/browse/OSSRH-95552)).

This team user account that was created also gives us access to the [Staging repository](https://s01.oss.sonatype.org/#stagingRepositories) which is used to validate Maven publications before they are published.

#### Handling Failed Publications

If the library fails to be published then it might have failed validation in the [Sonatype Staging repository](https://s01.oss.sonatype.org/#stagingRepositories) so check there for some clues.

#### Creating a Sonatype User

To get access to the Sonatype domain `uk.org.justice.service.hmpps`:

* [Create a Sonatype user account](https://issues.sonatype.org/secure/Signup!default.jspa)
* Get an existing Sonatype user with access to the domain to [raise a ticket](https://issues.sonatype.org/secure/CreateIssue.jspa) requesting access for the new user account. See [this Jira ticket](https://issues.sonatype.org/browse/OSSRH-95552) as an example.

#### Adding Credentials to a Publish Request

A valid Sonatype username and password are required to publish to Maven Central.

In `build.gradle.kts` we use environment variables `OSSRH_USERNAME` and `OSSRH_PASSWORD` to authenticate with Sonatype. These environment variables must be set when running the `publish` task.

Note that this means the environment variables have been [set in Circle CI](https://app.circleci.com/settings/project/github/ministryofjustice/hmpps-digital-prison-reporting-lib/environment-variables). This is safe as environment variables cannot be retrieved from Circle.

#### Changing the Sonatype Credentials

If you need to change the secrets used to authorise with Sonatype delete the Circle CI environment variables (`OSSRH_USERNAME` and `OSSRH_PASSWORD`) and re-add them with the username and password of another Sonatype user with access to the domain.

### Signing a Publish Request to Maven Central

One of the requirements for publishing to Maven Central is that all publications are [signed using PGP](https://central.sonatype.org/publish/requirements/gpg/).

#### Signing a Publication on Circle CI

In `build.gradle.kts` we use environment variables `ORG_GRADLE_PROJECT_signingKey` as recommended in the [Gradle Signing Plugin documentation](https://docs.gradle.org/current/userguide/signing_plugin.html#sec:in-memory-keys).

The current public signing key has been uploaded to [this public keyserver](https://keys.openpgp.org/) as this is a sonatype requirement and it is using it to validate the signature.

### Note: The current signing key has an expiration date of 16 October 2024 at 15:12.

#### Changing the Signing Key

* Generate a new key - follow the [Sonatype guide](https://central.sonatype.org/publish/requirements/gpg/).
* Export the private key to a file - google for `gpg export private key` and you should find several guides for using `gpg --export-secret-keys`.
* To allow the private key to be inserted into Circle, convert the newlines in the private key to `\n` see [this forum](https://discuss.circleci.com/t/gpg-keys-as-environment-variables/28641) if you have problems with this. 
* Delete the environment variables `ORG_GRADLE_PROJECT_signingKey` and `ORG_GRADLE_PROJECT_signingPassword` from the [Circle CI env vars page](https://app.circleci.com/settings/project/github/ministryofjustice/hmpps-digital-prison-reporting-lib/environment-variables)
* Recreate the environment variables where `ORG_GRADLE_PROJECT_signingKey` contains the private key (with newlines) and `ORG_GRADLE_PROJECT_signingPassword` contains the passphrase.  
