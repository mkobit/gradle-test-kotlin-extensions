package testsupport.stdlib

fun <T> List<T>.removeFirstSequence(sequence: List<T>): List<T> {
  val firstIndex = (0..(size - sequence.size))
    .first { containsElementAtIndex(it, sequence) }
  return subList(0, firstIndex) + subList(firstIndex + sequence.size, size)
}

private fun <T> List<T>.containsElementAtIndex(startingFromIndex: Int, elements: List<T>): Boolean {
  return (startingFromIndex until startingFromIndex + elements.size)
    .map { get(it) }
    .zip(elements)
    .all { (first, second) -> first == second }
}
