package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.missingReport

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(schema = "missingreportsubmission", name = "missing_report_submission")
class MissingReportSubmission(
  @Column(nullable = false, name = "userid")
  val userId: String,
  @Column(nullable = false, name = "reportid")
  val reportId: String,
  @Column(nullable = false, name = "reportvariantid")
  val reportVariantId: String,
  @Column(nullable = true)
  val reason: String?,
) {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  val id: Int? = null
}
