package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.parser.CronParser
import java.util.Locale

class CronExpressionInterpreter {

  companion object {
    private val cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(com.cronutils.model.CronType.QUARTZ)
    private val parser = CronParser(cronDefinition)
    private val descriptor = CronDescriptor.instance(Locale.UK)
    fun interpret(schedule: String?): String? = schedule?.let { expression ->
      try {
        val cron = parser.parse(expression)
        descriptor.describe(cron)
      } catch (e: IllegalArgumentException) {
        "Invalid expression"
      }
    }
  }
}
