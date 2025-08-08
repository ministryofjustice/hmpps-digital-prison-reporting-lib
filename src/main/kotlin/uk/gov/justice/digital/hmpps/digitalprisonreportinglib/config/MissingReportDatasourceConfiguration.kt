package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
  basePackages = ["uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport"],
  entityManagerFactoryRef = "missingReportEntityManagerFactory",
  transactionManagerRef = "missingReportTransactionManager",
)
class MissingReportDatasourceConfiguration {
  @Bean
  @ConfigurationProperties("spring.datasource.missingreport")
  fun missingReportDataSourceProperties() = DataSourceProperties()

  @Bean
  @ConfigurationProperties("spring.datasource.missingreport.hikari")
  fun missingReportDataSource(missingReportDataSourceProperties: DataSourceProperties) = missingReportDataSourceProperties.initializeDataSourceBuilder().build()

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
