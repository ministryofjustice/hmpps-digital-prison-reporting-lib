package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import com.zaxxer.hikari.HikariDataSource
import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

@Configuration
@AutoConfigureAfter(DataSourceAutoConfiguration::class)
@ConditionalOnProperty("spring.datasource.missingreport.url")
@EnableJpaRepositories(
  basePackages = ["uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport"],
  entityManagerFactoryRef = "missingReportEntityManagerFactory",
  transactionManagerRef = "missingReportTransactionManager",
)
class MissingReportDatasourceConfiguration(
  val environment: Environment,
) {

  @Bean
  @ConfigurationProperties("spring.datasource.missingreport.hikari")
  fun missingReportDataSource(
    missingReportDataSourceProperties: DataSourceProperties,
  ): DataSource {
    val properties = Binder.get(environment).bind("spring.datasource.missingreport", DataSourceProperties::class.java)
      .orElseThrow { IllegalStateException("No spring.datasource.missingreport config found.") }
    return properties
      .initializeDataSourceBuilder()
      .type(HikariDataSource::class.java)
      .build()
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
