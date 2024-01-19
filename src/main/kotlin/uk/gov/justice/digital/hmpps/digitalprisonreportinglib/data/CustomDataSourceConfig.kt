package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class CustomDataSourceConfig(
  @Value("\${spring.datasource.url}") private val url: String,
  @Value("\${spring.datasource.username}") private val username: String,
  @Value("\${spring.datasource.password}") private val password: String,
  @Value("\${spring.datasource.driver-class-name}") private val driver: String,
) {
  @Bean("customDataSource")
  fun createCustomDataSource(): DataSource {
    return DataSourceBuilder.create()
      .url(url)
      .username(username)
      .password(password)
      .driverClassName(driver)
      .build()
  }

}