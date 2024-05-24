package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.redshiftdata

data class StatementResult(
  val records: List<Map<String, Any?>>,
  val nextToken: String? = null,
)
