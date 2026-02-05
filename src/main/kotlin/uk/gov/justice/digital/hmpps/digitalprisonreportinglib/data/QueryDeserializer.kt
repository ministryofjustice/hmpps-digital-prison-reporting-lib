package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.Datasource
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.MultiphaseQuery
import java.lang.reflect.Type

class QueryDeserializer : JsonDeserializer<List<MultiphaseQuery>> {

  override fun deserialize(
    json: JsonElement,
    typeOfT: Type,
    context: JsonDeserializationContext
  ): List<MultiphaseQuery> {

    return when {
      json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
        // Old format: "query": ""
        listOf(
          MultiphaseQuery(
            index = 0,
            datasource = Datasource(
              id = "default",
              name = "default"
            ),
            query = json.asString
          )
        )
      }

      json.isJsonArray -> {
        // New format: "query": [ {  } ] array of MultiphaseQuery
        json.asJsonArray.map { element ->
          context.deserialize<MultiphaseQuery>(
            element,
            MultiphaseQuery::class.java
          )
        }
      }

      else -> {
        throw JsonParseException(
          "Invalid 'query' field. Expected string or array of multiphase queries."
        )
      }
    }
  }
}
