package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.controller

import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@Validated
@RestController
class OutOfMemoryTestController(
  val infiniteList: MutableList<ByteArray>,
) {

  @GetMapping("/testing/oom")
  fun testOutOfMemory(): String {
    infiniteList.add(ByteArray(5_000_000))
    return "stored ${infiniteList.size}"
  }
}
