package no.nav.mulighetsrommet.api.utils

object DatabaseUtils {
    fun andWhereParameterNotNull(vararg parts: Pair<Any?, String?>): String = parts
        .filter { it.first != null }
        .map { it.second }
        .reduceOrNull { where, part -> "$where and $part" }
        ?.let { "where $it" }
        ?: ""

    fun <T> paginate(operation: (Int) -> List<T>): Int {
        var offset = 1
        var count = 0

        do {
            val list = operation(offset)
            offset += 1
            count += list.size
        } while (list.isNotEmpty())

        return count
    }
}
