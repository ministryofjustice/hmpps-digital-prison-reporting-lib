package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.config

import org.springframework.boot.jackson.JacksonComponent
import tools.jackson.databind.ValueDeserializer
import uk.gov.justice.hmpps.kotlin.auth.AuthSource


@JacksonComponent
class CaseInsensitiveAuthSourceDeserializer : ValueDeserializer<AuthSource>() {
  override fun deserialize(
    p: tools.jackson.core.JsonParser,
    ctxt: tools.jackson.databind.DeserializationContext,
  ): AuthSource {
    val value = p.valueAsString ?: throw IllegalArgumentException("Enum value was null")

    return AuthSource.entries.firstOrNull {
      it.name.equals(value, ignoreCase = true)
    } ?: throw IllegalArgumentException("Invalid value $value for enum")
  }
}