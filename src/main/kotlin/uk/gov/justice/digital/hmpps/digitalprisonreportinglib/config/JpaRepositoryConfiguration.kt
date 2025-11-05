package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport.MissingReportDatasourceFilter

@EnableJpaRepositories(
  // Exclude missing reports because it uses a different datasource
  excludeFilters = [ComponentScan.Filter(type = FilterType.ANNOTATION, classes = [MissingReportDatasourceFilter::class])],
  basePackages = ["uk.gov.justice.digital.hmpps.digitalprisonreportinglib"],
)
@EntityScan(
  basePackages = ["uk.gov.justice.digital.hmpps.digitalprisonreportinglib"],
)
@Configuration
class JpaRepositoryConfiguration
