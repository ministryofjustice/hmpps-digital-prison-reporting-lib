package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

@Configuration
class DatasourceConfiguration {
  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  fun mainDataSourceProperties() = DataSourceProperties()

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  fun mainDataSource(mainDataSourceProperties: DataSourceProperties) = mainDataSourceProperties.initializeDataSourceBuilder().build()

  @Bean
  fun mainEntityManagerFactory(
    mainDataSource: DataSource,
  ): LocalContainerEntityManagerFactoryBean = LocalContainerEntityManagerFactoryBean().apply {
    setDataSource(mainDataSource)
    setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
    setPackagesToScan("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model")
  }

  @Bean
  fun mainTransactionManager(@Qualifier("mainEntityManagerFactory") mainEntityManagerFactory: LocalContainerEntityManagerFactoryBean) = JpaTransactionManager(mainEntityManagerFactory.getObject()!!)
}
