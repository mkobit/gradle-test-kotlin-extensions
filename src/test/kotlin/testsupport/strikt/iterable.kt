package testsupport.strikt

import strikt.api.Assertion

fun <T : Iterable<E>, E> Assertion.Builder<T>.containsSequence(vararg elements: E): Assertion.Builder<T> =
  containsSequence(elements.toList())

fun <T : Iterable<E>, E> Assertion.Builder<T>.containsSequence(elements: Collection<E>): Assertion.Builder<T> {
  return assert("contains sequence %s", elements) { subject ->
    if (elements.isEmpty()) {
      pass()
    } else if (!subject.iterator().hasNext()) {
      fail()
    } else {
      val subjectAsList = subject.toList()
      val elementsAsList = elements.toList()
      val anySequence = (0..(subjectAsList.size - elementsAsList.size))
        .any { subjectAsList.containsElementsAtIndex(it, elementsAsList) }
      if (anySequence) {
        pass()
      } else {
        fail()
      }
    }
  }
}

private fun <T> List<T>.containsElementsAtIndex(startingFromIndex: Int, elements: List<T>): Boolean {
  return (startingFromIndex until startingFromIndex + elements.size)
    .asSequence()
    .map { get(it) }
    .zip(elements.asSequence())
    .all { (first, second) -> first == second }
}
