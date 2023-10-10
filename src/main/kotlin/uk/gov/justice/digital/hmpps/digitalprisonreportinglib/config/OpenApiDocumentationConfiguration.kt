package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiDocumentationConfiguration {
  @Bean
  fun openAPIDocConfig(): OpenAPI? {
    return OpenAPI()
      .info(
        Info().title("Digital Prison Reporting MI API")
          .version("v1.0.0"),
      )
  }
}
