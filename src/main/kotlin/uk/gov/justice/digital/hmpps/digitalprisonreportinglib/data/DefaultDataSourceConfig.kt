package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ConfiguredApiRepository.Companion.DEFAULT_DATASOURCE
import javax.sql.DataSource

@Configuration
class DefaultDataSourceConfig(
  @Value("\${spring.datasource.url}") private val url: String,
  @Value("\${spring.datasource.username}") private val username: String,
  @Value("\${spring.datasource.password}") private val password: String,
  @Value("\${spring.datasource.driver-class-name}") private val driver: String,
) {

  @Autowired
  lateinit var context: ApplicationContext

  @ConditionalOnMissingBean
  @Bean(DEFAULT_DATASOURCE)
  fun createCustomDataSource(): DataSource {
    return DataSourceBuilder.create()
      .url(url)
      .username(username)
      .password(password)
      .driverClassName(driver)
      .build()
  }
}
