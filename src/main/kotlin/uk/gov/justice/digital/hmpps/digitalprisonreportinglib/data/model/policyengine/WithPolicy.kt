package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

sealed interface WithPolicy {
  val policy: List<Policy>
}
