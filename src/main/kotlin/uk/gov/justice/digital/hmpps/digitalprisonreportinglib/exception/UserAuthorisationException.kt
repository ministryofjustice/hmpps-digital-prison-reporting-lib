package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception

class UserAuthorisationException(val reason: String) : RuntimeException(reason)
