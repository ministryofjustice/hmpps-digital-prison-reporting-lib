package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.AutoConfigureBefore
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.invoke
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ConfiguredApiController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.ReportDefinitionController
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ConfiguredApiService
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.ReportDefinitionService

// @ConditionalOnMissingClass
@Configuration
@AutoConfigureBefore(WebFluxAutoConfiguration::class, WebMvcAutoConfiguration::class)
//@EnableAutoConfiguration
class ControllerConfig {

  @Bean
  fun configuredApiController(configuredApiService: ConfiguredApiService): ConfiguredApiController {
    return ConfiguredApiController(configuredApiService)
  }

  @Bean
  fun reportDefinitionController(reportDefinitionService: ReportDefinitionService): ReportDefinitionController {
    return ReportDefinitionController(reportDefinitionService)
  }
}
