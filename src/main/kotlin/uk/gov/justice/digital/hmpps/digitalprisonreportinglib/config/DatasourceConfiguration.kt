package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.hibernate.jpa.HibernatePersistenceProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
  basePackages = ["uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data"],
  entityManagerFactoryRef = "mainEntityManagerFactory",
  transactionManagerRef = "mainTransactionManager",
)
class LibConfiguration {
  @Bean
  @ConfigurationProperties("spring.datasource")
  fun mainDataSourceProperties() = DataSourceProperties()

  @Bean
  @ConfigurationProperties("spring.datasource.hikari")
  fun mainDataSource(mainDataSourceProperties: DataSourceProperties) = mainDataSourceProperties.initializeDataSourceBuilder().build()

  @Bean
  @Primary
  fun mainEntityManagerFactory(
    mainDataSource: DataSource,
    @Value("#{'\${dpr.lib.modelPackages:}'.split(',')}") modelPackages: List<String>,
  ): LocalContainerEntityManagerFactoryBean {
    val mutableModelPackages = modelPackages.filter { it != "" }.toMutableList()
    if (!mutableModelPackages.contains("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model")) {
      mutableModelPackages.add("uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model")
    }
    return LocalContainerEntityManagerFactoryBean().apply {
      setDataSource(mainDataSource)
      setPersistenceProviderClass(HibernatePersistenceProvider::class.java)
      setPackagesToScan(
        *mutableModelPackages.toTypedArray(),
      )
    }
  }

  @Bean
  fun mainTransactionManager(@Qualifier("mainEntityManagerFactory") mainEntityManagerFactory: LocalContainerEntityManagerFactoryBean) = JpaTransactionManager(mainEntityManagerFactory.getObject()!!)
}

/**
 * This exists so that apps using this library can have their own repositories and entities be recognised by Spring if wanted
 */
@Configuration
@ConditionalOnProperty(value = ["dpr.lib.repositoryPackage", "dpr.lib.modelPackages"])
@EnableJpaRepositories(
  basePackages = ["\${dpr.lib.repositoryPackage}"],
  entityManagerFactoryRef = "mainEntityManagerFactory",
  transactionManagerRef = "mainTransactionManager",
)
class AppConfiguration
