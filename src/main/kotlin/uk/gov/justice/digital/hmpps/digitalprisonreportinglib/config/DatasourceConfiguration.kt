package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class DatasourceConfiguration {
  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  fun mainDataSourceProperties() = DataSourceProperties()

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  fun mainDataSource(mainDataSourceProperties: DataSourceProperties) = mainDataSourceProperties.initializeDataSourceBuilder().build()
}
