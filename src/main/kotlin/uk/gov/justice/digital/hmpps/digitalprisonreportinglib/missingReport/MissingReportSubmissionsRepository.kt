package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Repository

@Repository
@ConditionalOnBean(MissingReportService::class)
class MissingReportSubmissionsRepository(
  @PersistenceContext(unitName = "missingreportsubmission")
  private val entityManager: EntityManager,
) {

  fun save(missingReport: MissingReportSubmission): MissingReportSubmission = entityManager.merge(missingReport)
}
