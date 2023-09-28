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

Provides a front end for Management Information Visualisation and Presentation

## Local Development

This project uses gradle which is bundled with the repository and also makes use
of

- [micronaut](https://micronaut.io/) - for compile time dependency injection
- [lombok](https://projectlombok.org/) - to reduce boilerplate when creating data classes
- [jacoco](https://docs.gradle.org/current/userguide/jacoco_plugin.html) - for test coverage reports

## Running Locally against Dev
1. Add implementation("com.h2database:h2:2.1.214") to build.gradle
2. Change the existing datasource config in the application.yml file to the following:
```
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: create-drop
  datasource:
    url: jdbc:h2:mem:datamart;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS domain\;
    username: sa
    password: sa
    driver-class-name: org.h2.Driver
```
3. Add the following two environment variables on intellij run configuration
    ```HMPPS_AUTH_URL https://sign-in-dev.hmpps.service.justice.gov.uk/auth```
    <br/><br/>
    ```AUTHORISED_ROLES ROLE_PRISONS_REPORTING_USER```
4. Optional: Change the org.springframework.security level to DEBUG in logback-spring.xml
5. Run main from DigitalPrisonReportingMi

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
- Otherwise use the prefix `NOJIRA`

### Pull Requests

- Reference or link any relevant JIRA tickets in the pull request notes
- At least one approval is required before a PR can be merged

## Deployment

The app is deployed to the namespace: `hmpps-digital-prison-reporting-mi-<env>`.

Config for the dev environment can be found here: https://github.com/ministryofjustice/cloud-platform-environments/tree/main/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-digital-prison-reporting-mi-dev

Additionally, the RedShift credentials need to be manually deployed to each environment. The file `redshift-jdbc-secret.yaml` should be updated with the base64 encoded values and applied to the environment.

_NB: Please do not commit these changes to `redshift-jdbc-secret.yaml`._

Example of base64 encoding a secret value:

```
echo -n 'placeholder' | base64
```

Example of applying the secret to an environment:

```
kubectl -n hmpps-digital-prison-reporting-mi-dev apply -f redshift-jdbc-secret.yaml
```