package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception

class MissingTableException(val tableId: String) : RuntimeException("Table reports.$tableId not found.")
