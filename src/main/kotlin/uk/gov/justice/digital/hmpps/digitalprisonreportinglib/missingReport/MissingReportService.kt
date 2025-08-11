package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.MissingReportSubmissionRequest

@Service
@Transactional(transactionManager = "missingReportTransactionManager")
class MissingReportService(
  private val missingReportSubmissionsRepository: MissingReportSubmissionsRepository,
) {
  fun createMissingReportSubmission(
    missingReportSubmissionRequest: MissingReportSubmissionRequest,
  ): MissingReportSubmission {
    val (userId, reportId, reportVariantId, reason) = missingReportSubmissionRequest
    val submission = MissingReportSubmission(userId, reportId, reportVariantId, reason)
    return missingReportSubmissionsRepository.saveAndFlush(submission)
  }
}
