package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.then
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.RenderMethod
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.ReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.SingleVariantReportDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller.model.VariantDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.ProductDefinitionRepository
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Dataset
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MetaData
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.RenderMethod.HTML
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Report
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Schema
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.SingleReportProductDefinition
import java.time.LocalDate

class ReportDefinitionServiceTest {

  val minimalDefinition = ProductDefinition(
    id = "1",
    name = "2",
    metadata = MetaData(
      author = "3",
      owner = "4",
      version = "5",
    ),
    report = emptyList(),
  )

  val minimalSingleDefinition = SingleReportProductDefinition(
    id = "1",
    name = "2",
    report = Report(
      id = "3",
      name = "4",
      created = LocalDate.now(),
      version = "5",
      dataset = "\$ref:10",
      render = HTML,
    ),
    dataset = Dataset(
      id = "10",
      name = "11",
      query = "12",
      schema = Schema(emptyList()),
    ),
    datasource = Datasource(
      id = "20",
      name = "21",
    ),
    metadata = MetaData(
      author = "30",
      version = "31",
      owner = "32",
    ),
  )

  @Test
  fun `Getting report list for user maps correctly`() {
    val expectedResult = ReportDefinition(
      id = "1",
      name = "2",
      variants = listOf(
        VariantDefinition(
          id = "1",
          name = "2",
          resourceName = "3",
        ),
      ),
    )
    val caseLoads = listOf("caseLoad")

    val repository = mock<ProductDefinitionRepository> {
      on { getProductDefinitions() } doReturn listOf(minimalDefinition)
    }
    val mapper = mock<ReportDefinitionMapper> {
      on { map(any(), any(), any(), any()) } doReturn expectedResult
    }
    val service = ReportDefinitionService(repository, mapper)

    val actualResult = service.getListForUser(RenderMethod.HTML, 20, caseLoads)

    then(repository).should().getProductDefinitions()
    then(mapper).should().map(minimalDefinition, RenderMethod.HTML, 20, caseLoads)

    assertThat(actualResult).isNotEmpty
    assertThat(actualResult).hasSize(1)
    assertThat(actualResult[0]).isEqualTo(expectedResult)
  }

  @Test
  fun `Getting single report for user maps correctly`() {
    val expectedResult = SingleVariantReportDefinition(
      id = "1",
      name = "2",
      variant = VariantDefinition(
        id = "1",
        name = "2",
        resourceName = "3",
      ),
    )
    val caseLoads = listOf("caseLoad")

    val repository = mock<ProductDefinitionRepository> {
      on { getSingleReportProductDefinition(any(), any()) } doReturn minimalSingleDefinition
    }
    val mapper = mock<ReportDefinitionMapper> {
      on { map(any(), any(), any()) } doReturn expectedResult
    }
    val service = ReportDefinitionService(repository, mapper)

    val actualResult = service.getDefinition(
      minimalSingleDefinition.id,
      minimalSingleDefinition.report.id,
      20,
      caseLoads,
    )

    then(repository).should().getSingleReportProductDefinition(minimalSingleDefinition.id, minimalSingleDefinition.report.id)
    then(mapper).should().map(minimalSingleDefinition, 20, caseLoads)

    assertThat(actualResult).isNotNull
    assertThat(actualResult).isEqualTo(expectedResult)
  }

  @Test
  fun `Getting HTML report list with no matches returns no domains`() {
    val definitionWithNoVariants = ReportDefinition(
      id = "1",
      name = "2",
      variants = emptyList(),
    )
    val caseLoads = listOf("caseLoad")
    val repository = mock<ProductDefinitionRepository> {
      on { getProductDefinitions() } doReturn listOf(minimalDefinition)
    }
    val mapper = mock<ReportDefinitionMapper> {
      on { map(any(), any(), any(), any()) } doReturn definitionWithNoVariants
    }
    val service = ReportDefinitionService(repository, mapper)

    val actualResult = service.getListForUser(RenderMethod.HTML, 20, caseLoads)

    assertThat(actualResult).hasSize(0)
  }
}
