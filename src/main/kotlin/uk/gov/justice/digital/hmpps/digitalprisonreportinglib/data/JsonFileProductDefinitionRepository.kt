package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import jakarta.validation.ValidationException
import uk.gov.justice.digital.hmpps.digitalprisonreportinglib.data.model.ProductDefinition

class JsonFileProductDefinitionRepository(
  private val resourceLocations: List<String>,
  private val gson: Gson,
) : AbstractProductDefinitionRepository() {

  override fun getProductDefinitions(path: String?): List<ProductDefinition> {
    return resourceLocations.map { gson.fromJson(this::class.java.classLoader.getResource(it)?.readText(), object : TypeToken<ProductDefinition>() {}.type) }
  }
}
