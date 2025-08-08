package uk.gov.justice.digital.hmpps.digitalprisonreportinglib

import jakarta.annotation.PostConstruct
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.MigrationVersion
import org.springframework.boot.test.context.TestConfiguration
import javax.sql.DataSource


@TestConfiguration
class TestFlywayConfig(private val mainDataSource: DataSource) {
  @PostConstruct
  fun init() {
    Flyway.configure()
      .dataSource(mainDataSource)
      .schemas("domain")
      .baselineOnMigrate(true)
      .target(MigrationVersion.LATEST)
      .locations("classpath:migration/test")
      .load().migrate()
  }
}