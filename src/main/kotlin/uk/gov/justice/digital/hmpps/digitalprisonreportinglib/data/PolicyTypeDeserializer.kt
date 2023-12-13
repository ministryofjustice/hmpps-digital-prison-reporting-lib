package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.policyengine.PolicyType
import java.lang.reflect.Type

class PolicyTypeDeserializer : JsonDeserializer<PolicyType?> {
  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): PolicyType {
    val stringValue = json.asString
    return PolicyType.entries.firstOrNull { enum -> enum.type == stringValue }
      ?: throw IllegalArgumentException("Unknown ParameterType $stringValue!")
  }
}
