package epicarchitect.thelastarch

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

object Network {
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    val pokemons = PokemonsApi(httpClient)
}