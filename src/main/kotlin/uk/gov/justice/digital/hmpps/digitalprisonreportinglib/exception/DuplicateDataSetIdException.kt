package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception

class DuplicateDataSetIdException(val dataSetId: String) : RuntimeException("Error: Duplicate dataSet Id found: $dataSetId")
