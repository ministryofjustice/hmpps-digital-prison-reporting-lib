package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty("spring.datasource.missingreport.url")
class MissingReportDatasourceConfiguration(
  val environment: Environment,
) {

  @Bean
  fun missingReportDataSource(): DataSource {
    val properties = Binder.get(environment).bind("spring.datasource.missingreport", DataSourceProperties::class.java)
      .orElseThrow { IllegalStateException("No spring.datasource.missingreport config found.") }
    val hikariConfig = Binder.get(environment)
      .bind("spring.datasource.missingreport.hikari", HikariConfig::class.java)
      .orElse(HikariConfig())

    hikariConfig.jdbcUrl = properties.url
    hikariConfig.username = properties.username
    hikariConfig.password = properties.password
    hikariConfig.driverClassName = properties.driverClassName

    return HikariDataSource(hikariConfig)
  }

  @Bean
  fun missingReportEntityManagerFactory(missingReportDataSource: DataSource) = LocalContainerEntityManagerFactoryBean().apply {
    setDataSource(missingReportDataSource)
    setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
    persistenceUnitName = "missingreportsubmission"
    setPackagesToScan("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport")
  }

  @Bean
  fun missingReportTransactionManager(@Qualifier("missingReportEntityManagerFactory") missingReportEntityManagerFactory: LocalContainerEntityManagerFactoryBean) = JpaTransactionManager(missingReportEntityManagerFactory.getObject()!!)
}
