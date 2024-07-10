package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model

import com.fasterxml.jackson.annotation.JsonValue

enum class SummaryTemplate(@JsonValue val template: String) {
  TableHeader("table-header"),
  TableFooter("table-footer"),
  SectionFooter("section-footer"),
  PageHeader("page-header"),
  PageFooter("page-footer"),
}
