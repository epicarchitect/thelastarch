package epicarchitect.thelastarch.tools

data class PagingData<DATA: Any>(
    val items: List<DATA>,
    val error: Throwable? = null,
    val loading: Boolean = false
)

object LoadingItem

data class ErrorItem(val text: String)

fun <DATA: Any> PagingData<DATA>.toEpicItems() = when {
    error != null -> items + ErrorItem(error.localizedMessage ?: "error")
    loading -> items + LoadingItem
    else -> items
}

fun <DATA : Any> createLoadingPagingData(items: List<DATA>) = PagingData(
    items = items.orEmpty(),
    error = null,
    loading = true
)

suspend fun <DATA : Any> appendPagingData(
    previousData: PagingData<DATA>?,
    request: suspend () -> List<DATA>
) = try {
    val lastList = previousData?.items ?: emptyList()
    PagingData(
        items = lastList + request(),
        error = null,
        loading = false
    )
} catch (throwable: Throwable) {
    PagingData(
        items = previousData?.items.orEmpty(),
        error = throwable,
        loading = false
    )
}