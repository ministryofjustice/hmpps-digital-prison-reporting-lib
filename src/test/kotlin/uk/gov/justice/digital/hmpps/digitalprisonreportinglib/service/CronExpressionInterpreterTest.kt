package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CronExpressionInterpreterTest {

  @Test
  fun `Interpret daily expression works ok`() {
    val actual = CronExpressionInterpreter.interpret("0 0 12 * * ?")
    assertEquals("at 12:00", actual)
  }

  @Test
  fun `Interpret daily expression weekdays works ok`() {
    val actual = CronExpressionInterpreter.interpret("0 15 10 ? * MON-FRI")
    assertEquals("at 10:15 every day between Monday and Friday", actual)
  }

  @Test
  fun `Interpret incorrect daily expression as empty`() {
    val actual = CronExpressionInterpreter.interpret("0 15 10 ?")
    assertEquals("Invalid expression", actual)
  }
}
