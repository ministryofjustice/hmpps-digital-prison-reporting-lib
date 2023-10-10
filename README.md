# hmpps-digital-prison-reporting-mi-lib
Common library to create reports.

[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-digital-prison-reporting-mi)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-digital-prison-reporting-mi "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-digital-prison-reporting-mi/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-digital-prison-reporting-mi)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-digital-prison-reporting-mi/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-digital-prison-reporting-mi)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-digital-prison-reporting-mi-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

This project is generated from ministryofjustice/hmpps-template-kotlin

Requires Java 17 or above

#### CODEOWNER

- Team : [hmpps-digital-prison-reporting](https://github.com/orgs/ministryofjustice/teams/hmpps-digital-prison-reporting)
- Email : digitalprisonreporting@digital.justice.gov.uk

## Overview



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

### Integrating to your Spring Boot project
To integrate the library into your project you will need to add the dependency to your build.gradle file, e.g:
`implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:1.0.0")`

You will also need to add the following to your Spring Boot application class:
`@ComponentScan("yourapplicationpackage","uk.gov.justice.digital.hmpps.digitalprisonreportinglib")`
Where you will need to replace "yourapplicationpackage" with the actual package of your application.
