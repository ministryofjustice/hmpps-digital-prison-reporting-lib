package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import java.lang.reflect.Type

class QueryDeserializer : JsonDeserializer<List<MultiphaseQuery>> {

  companion object {
    // This is not used in single element MultiphaseQuery lists. It is here for compatibility with multiple query element execution as the datasource is required and used in MultiphaseQuery in this case.
    const val PLACEHOLDER_DATASOURCE = "PLACEHOLDER_DATASOURCE"
  }

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext,
  ): List<MultiphaseQuery> = when {
    json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
      listOf(
        MultiphaseQuery(
          index = 0,
          datasource = PLACEHOLDER_DATASOURCE,
          query = json.asString,
        ),
      )
    }
    json.isJsonArray -> {
      json.asJsonArray.map { element ->
        context.deserialize<MultiphaseQuery>(
          element,
          MultiphaseQuery::class.java,
        )
      }
    }
    else -> {
      throw JsonParseException(
        "Invalid 'query' field. Expected string or array of MultiphaseQuery.",
      )
    }
  }
}
