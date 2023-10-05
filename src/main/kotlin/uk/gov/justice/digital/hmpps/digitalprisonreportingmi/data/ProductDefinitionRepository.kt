package uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.digitalprisonreportingmi.data.model.ProductDefinition
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class ProductDefinitionRepository {

  fun getProductDefinitions(): List<ProductDefinition> {
    val gson: Gson = GsonBuilder()
      .registerTypeAdapter(LocalDate::class.java, LocalDateTypeAdapter())
      .create()
    return gson.fromJson(this::class.java.classLoader.getResource("productDefinition.json")?.readText(), object : TypeToken<List<ProductDefinition>>() {}.type)
  }
}
class LocalDateTypeAdapter : JsonSerializer<LocalDate?>, JsonDeserializer<LocalDate?> {
  private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  override fun serialize(
    date: LocalDate?,
    typeOfSrc: Type?,
    context: JsonSerializationContext?,
  ): JsonElement {
    return JsonPrimitive(date?.format(formatter))
  }

  @Throws(JsonParseException::class)
  override fun deserialize(
    json: JsonElement,
    typeOfT: Type?,
    context: JsonDeserializationContext?,
  ): LocalDate {
    return LocalDate.parse(json.asString, formatter)
  }
}
