package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine

data class Policy(val id: String, val type: PolicyType, val action: List<String>, val rule: List<Rule>)
