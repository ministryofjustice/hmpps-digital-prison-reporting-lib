package uk.gov.justice.digital.hmpps.digitalprisonreportinglib.subscription

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Predicate
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.stereotype.Repository

@Repository
@ConditionalOnBean(UserSubscriptionService::class)
class UserSubscriptionRepository(
  @PersistenceContext(unitName = "usersubscription")
  private val entityManager: EntityManager,
) {

  open fun findByUserId(userId: String) : List<UserSubscription> {
    entityManager.find<UserSubscription>()
  }

  open fun findByUserIdAndReport(userId: String, reportId: String, reportVariantId: String) : UserSubscription? {

  }

  private fun buildQuery(userId: String, reportId: String?, reportVariantId: String?) : List<UserSubscription> {

    val cb = entityManager.criteriaBuilder
    val query = cb.createQuery(UserSubscription::class)
    val user = query.from(UserSubscription::class)
    val predicates = mutableList<Predicate>()

    // Kotlin's safe-calls (?.) and takeIf clean up null/empty checking
    name?.takeIf { it.isNotBlank() }?.let { predicates.add(cb.equal(user.get<String>("name"), it)) }
    city?.takeIf { it.isNotBlank() }?.let { predicates.add(cb.equal(user.get<String>("city"), it)) }
    role?.takeIf { it.isNotBlank() }?.let { predicates.add(cb.equal(user.get<String>("role"), it)) }

    query.where(*predicates.toTypedArray()) // Kotlin spread operator converts List to vararg
    return em.createQuery(query).resultList
  }

    fun save(userSubscription: UserSubscription)
}