package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception

class TableExpiredException(val tableId: String) : RuntimeException("Table reports.$tableId has expired.")
