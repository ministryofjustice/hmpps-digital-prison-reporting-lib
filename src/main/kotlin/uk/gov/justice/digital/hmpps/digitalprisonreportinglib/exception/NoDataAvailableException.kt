package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception

class NoDataAvailableException(val reason: String) : RuntimeException(reason)
