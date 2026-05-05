package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception

class ExecutionStatementNotFound(val statementId: String, val detailMessage: String?) : RuntimeException("Statement $statementId not found. $detailMessage")
