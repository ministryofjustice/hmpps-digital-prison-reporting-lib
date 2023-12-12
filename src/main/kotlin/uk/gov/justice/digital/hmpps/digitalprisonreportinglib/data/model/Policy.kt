package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model

data class Policy(val id: String, val type: String, val action: List<String>, val rule: List<Rule>)
