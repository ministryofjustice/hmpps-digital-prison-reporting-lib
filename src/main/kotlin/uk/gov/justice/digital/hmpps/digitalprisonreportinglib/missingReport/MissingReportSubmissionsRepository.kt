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

  @Transactional(
    value = "missingReportTransactionManager",
    readOnly = true,
  )
  fun findById(id: Int): MissingReportSubmission? = entityManager.find(MissingReportSubmission::class.java, id)

  @Transactional(
    value = "missingReportTransactionManager",
    readOnly = true,
  )
  fun findMissingReportSubmission(
    reportId: String,
    variantId: String,
    userId: String,
  ): MissingReportSubmission? = entityManager.createQuery(
    """
      select m
      from MissingReportSubmission m
      where m.userId = :userId
        and m.reportId = :reportId
        and m.reportVariantId = :variantId
      """,
    MissingReportSubmission::class.java,
  )
    .setParameter("userId", userId)
    .setParameter("reportId", reportId)
    .setParameter("variantId", variantId)
    .resultList
    .firstOrNull()
}
