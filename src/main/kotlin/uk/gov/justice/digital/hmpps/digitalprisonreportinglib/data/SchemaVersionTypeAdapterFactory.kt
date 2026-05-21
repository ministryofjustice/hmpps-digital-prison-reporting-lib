package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.AnyProductDefinition
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.exception.UnsupportedVersionException

class SchemaVersionTypeAdapterFactory : TypeAdapterFactory {
  override fun <T : Any> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
    val rawType = type.rawType
    if (!AnyProductDefinition::class.java.isAssignableFrom(rawType)) {
      return null
    }
    val delegate = gson.getDelegateAdapter(this, type)
    val elementAdapter = gson.getAdapter(JsonElement::class.java)

    return object : TypeAdapter<T>() {
      override fun write(out: JsonWriter, value: T) {
        delegate.write(out, value)
      }

      override fun read(input: JsonReader): T {
        val jsonElement = elementAdapter.read(input)
        if (jsonElement.isJsonObject) {
          val jsonObject = jsonElement.asJsonObject
          var version = "1.0.0"
          if (jsonObject.has("schemaversion")) {
            version = jsonObject.get("schemaversion").asString
          }
          if (!(version.equals("1.0.0"))) {
            throw UnsupportedVersionException("Unsupported schemaversion: $version: Only 1.0.0 is supported at this time")
          }
        }
        return delegate.fromJsonTree(jsonElement)
      }
    }
  }
}
