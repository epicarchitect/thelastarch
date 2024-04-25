package epicarchitect.thelastarch.data.remote

import okhttp3.Response

data class NetworkResponse<DATA>(
    val httpResponse: Response,
    val data: DATA?
)