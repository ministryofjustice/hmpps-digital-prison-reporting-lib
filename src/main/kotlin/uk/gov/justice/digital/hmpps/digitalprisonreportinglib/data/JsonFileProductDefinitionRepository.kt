package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition
import java.time.LocalDate

class JsonFileProductDefinitionRepository(
  private val localDateTypeAdaptor: LocalDateTypeAdaptor,
  private val resourceLocation: String,
) : ProductDefinitionRepository {

  override fun getProductDefinitions(): List<ProductDefinition> {
    val gson: Gson = GsonBuilder()
      .registerTypeAdapter(LocalDate::class.java, localDateTypeAdaptor)
      .create()
    return gson.fromJson(this::class.java.classLoader.getResource(resourceLocation)?.readText(), object : TypeToken<List<ProductDefinition>>() {}.type)
  }
}
