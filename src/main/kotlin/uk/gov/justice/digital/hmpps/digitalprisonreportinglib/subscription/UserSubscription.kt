package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class UserSubscriptionStatus{
  SUBSCRIBED,
  UNSUBSCRIBED;
}

@Entity
@Table(schema = "usersubscription", name = "user_subscription")
class UserSubscription {
  @Column(nullable = false, name = "userid")
  val userId: String,
  @Column(nullable = false, name = "reportid")
  val reportId: String,
  @Column(nullable = false, name = "reportvariantid")
  val reportVariantId: String,
  @Column(nullable = false, name = "createdAt")
  val status: String,
  @Column(nullable = false, name = "status")
  val createdAt: LocalDateTime,
  @Column(nullable = true, name = "updatedAt")
  val updatedAt: LocalDateTime,
} {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  var id: Int? = null
}