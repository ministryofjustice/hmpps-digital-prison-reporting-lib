package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@EnableJpaRepositories(
  basePackages = ["uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data"],
)
@Configuration
class JpaRepositoryConfiguration
