package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import kotlinx.serialization.SerializationException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionGsonConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config.DefinitionKotlinxConfig
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UnsupportedVersionException

class ProductDefinitionParserTest {

  private val parser = ProductDefinitionParser(
    dprDefinitionGson =
    DefinitionGsonConfig()
      .definitionGson(IsoLocalDateTimeTypeAdaptor()),
    dprDefinitionKotlinx =
    DefinitionKotlinxConfig().kotlinxJson(),
  )

  @Test
  fun `lenient DPD without schemaversion is parsed with Gson`() {
    val lenientDpdJson = readResource("productDefinitionNoSchemaVersion.json")

    val parsed = parser.parse(lenientDpdJson)

    assertThat(parsed.definition.id).isEqualTo("external-movements")
    assertThat(parsed.summary.id).isEqualTo(parsed.definition.id)
  }

  @Test
  fun `lenient DPD with schemaversion 1_0_0 is parsed with Gson`() {
    val lenientDpdJson = readResource("productDefinitionValidVersion1_0_0.json")

    val parsed = parser.parse(lenientDpdJson)

    assertThat(parsed.definition.id).isEqualTo("external-movements")
    assertThat(parsed.summary.id).isEqualTo(parsed.definition.id)
  }

  @Test
  fun `lenient DPD with schemaversion 1_1_0 is rejected by Kotlinx`() {
    val lenientDpdJson = readResource("productDefinitionLenientBut1_1_0.json")

    assertThatThrownBy { parser.parse(lenientDpdJson) }.isInstanceOf(SerializationException::class.java)
  }

  @Test
  fun `strict DPD with schemaversion 1_1_0 is parsed successfully with Kotlinx`() {
    val json = readResource("strictlyValidDefinition.json")

    val parsed = parser.parse(json)

    assertThat(parsed.definition.id).isEqualTo("external-movements-strict")
    assertThat(parsed.definition.schemaVersion).isEqualTo("1.1.0")

    assertThat(parsed.summary).isEqualTo(parsed.definition.toSummary())
  }

  @Test
  fun `strict DPD with unsupported schemaversion 1_2_0 is rejected`() {
    val json = readResource("productDefinitionInvalidVersion.json")

    assertThatThrownBy { parser.parse(json) }
      .isInstanceOf(UnsupportedVersionException::class.java)
      .hasMessage(
        "Unsupported schemaversion: '1.2.0'. " +
          "Supported versions are 1.0.0 and 1.1.0.",
      )
  }

  @Test
  fun `strict DPD with null schemaversion is rejected`() {
    val json = readResource("productDefinitionNullVersion.json")

    assertThatThrownBy { parser.parse(json) }
      .isInstanceOf(InvalidProductDefinitionException::class.java)
      .hasMessage("schemaversion must be a JSON string")
  }

  private fun readResource(resourceLocation: String): String {
    val resource = requireNotNull(javaClass.classLoader.getResource(resourceLocation)) {
      "Test resource not found: $resourceLocation"
    }

    return resource.readText()
  }
}
