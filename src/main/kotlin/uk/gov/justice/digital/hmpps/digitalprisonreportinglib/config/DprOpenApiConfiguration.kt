package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class DprOpenApiConfiguration {
  @Bean("dprOpenAPIConfiguration")
  @ConditionalOnMissingBean(OpenAPI::class)
  fun openAPIConfiguration(): OpenAPI {
    return OpenAPI()
      .components(
        Components()
          .addSecuritySchemes(
            "bearer-jwt",
            SecurityScheme()
              .type(SecurityScheme.Type.HTTP)
              .scheme("bearer")
              .bearerFormat("JWT"),
          ),
      )
  }
}
