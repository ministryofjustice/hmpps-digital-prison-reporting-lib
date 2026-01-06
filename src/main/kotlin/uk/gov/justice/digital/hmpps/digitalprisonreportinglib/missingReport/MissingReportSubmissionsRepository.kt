package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class MissingReportSubmissionsRepository(
  private val entityManager: EntityManager,
) {

  @Transactional(value = "missingReportTransactionManager")
  fun save(missingReport: MissingReportSubmission): MissingReportSubmission = entityManager.merge(missingReport)
}
