## Integrating into your Spring Boot project
To integrate the library into your project you will need to add the dependency to your build.gradle file, e.g:
`implementation("uk.gov.justice.service.hmpps:hmpps-digital-prison-reporting-lib:1.0.0")`

You will also need to add the following to your Spring Boot application class:
`@ComponentScan("yourapplicationpackage","uk.gov.justice.digital.hmpps.digitalprisonreportinglib")`
Where you will need to replace "yourapplicationpackage" with the actual package of your application.

You will also need to use or extended the AuthAwareAuthenticationToken class in your Spring Security configuration as your Authentication implementation in order to pass the list of active caseload ids since this is used
for row level security in the library. 
The JWT token is needed to retrieve the caseload user details and this can be done simply by calling the getActiveCaseloadIds method of the CaseloadProvider and passing the JWT as a parameter.
An example can be found [here](https://github.com/ministryofjustice/hmpps-digital-prison-reporting-mi/blob/main/src/main/kotlin/uk/gov/justice/digital/hmpps/digitalprisonreportingmi/security/AuthAwareTokenConverter.kt#L15).
The AuthAwareAuthenticationToken can then be used in your controllers to retrieve the caseload ids should you need to implement row level security. 

### Open API Docs
The API documentation is generated via the following dependency:
`implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")`
By using this for your Open API configuration you will be able to see the API docs on the swagger endpoint exposed by your application.

### Overriding components and default implementations

The following interfaces have default implementations that can be implemented in the client application and/or configured:

| Interface                           | Purpose                                                                                              | Default implementation                                                                                                                          | Default configuration                                                                                                                                                                                                                                                          |
|-------------------------------------|------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AuthAwareTokenConverter             | Convert a JWT token to an `AuthAwareAuthenticationToken`, and delegates to the `CaseloadProvider`.   | `DefaultAuthAwareTokenConverter`: Extracts roles and delegates caseload provision - uses the `CaseloadProvider` implementation in the context.  | N/A                                                                                                                                                                                                                                                                            |
| CaseloadProvider                    | Provides the caseloads the current user has access to.                                               | `DefaultCaseloadProvider`: Requests caseloads from the configured endpoint.                                                                     | Set `dpr.lib.caseloads.host` to the Nomis User API host. Optionally set `dpr.lib.caseloads.path` (defaults to `me/caseloads`).                                                                                                                                                 |
| LocalDateTypeAdaptor                | Converts JSON date-times to the required format.                                                     | `IsoLocalDateTypeAdaptor`: Converts to the format "yyyy-MM-dd".                                                                                 | N/A                                                                                                                                                                                                                                                                            |
| ProductDefinitionRepository         | Returns a list of product definitions to be used by the API.                                         | `JsonFileProductDefinitionRepository`: Uses a list of JSON file sources for the product definitions.                                            | Set `dpr.lib.definition.locations` to a comma separated list of the locations of the source files (defaults to `productDefinition.json`, which can be created in the client application).                                                                                      |
| N/A (Spring Security configuration) | Ensure only authorised users have access to the API, and delegates to the `AuthAwareTokenConverter`. | `ResourceServerConfiguration`: Requires the user to have the specified role - uses the `AuthAwareTokenConverter` implementation in the context. | Set `dpr.lib.user.role` to the required user role. If not set, this implementation is disabled. Additionally, `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` should be set to the authentication server's JWKS file (e.g. `${hmpps.auth.url}/.well-known/jwks.json`). |
