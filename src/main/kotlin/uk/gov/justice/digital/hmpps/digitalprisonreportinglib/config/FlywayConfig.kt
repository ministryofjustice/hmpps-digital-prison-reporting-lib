package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
@ConditionalOnProperty("spring.datasource.missingreport.url")
class FlywayConfig(private val missingReportDataSource: DataSource) {
  @PostConstruct
  fun init() {
    Flyway.configure()
      .dataSource(missingReportDataSource)
      .schemas("missingreportsubmission")
      .baselineOnMigrate(true)
      .target(MigrationVersion.LATEST)
      .locations("classpath:migration/common")
      .load().migrate()
  }
}
