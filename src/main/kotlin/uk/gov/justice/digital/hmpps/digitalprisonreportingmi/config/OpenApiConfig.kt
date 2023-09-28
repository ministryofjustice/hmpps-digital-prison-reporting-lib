package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
  @Bean
  fun openAPI(): OpenAPI? {
    return OpenAPI()
      .info(
        Info().title("Digital Prison Reporting MI API")
          .version("v1.0.0"),
      )
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
