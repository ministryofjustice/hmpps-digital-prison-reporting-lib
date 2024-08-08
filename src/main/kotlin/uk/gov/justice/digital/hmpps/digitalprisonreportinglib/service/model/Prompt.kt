package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service.model

import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.FilterType

data class Prompt(val name: String, val value: String, val type: FilterType)
