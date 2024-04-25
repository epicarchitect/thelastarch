package epicarchitect.thelastarch.data.remote

import android.net.Uri
import androidx.core.net.toUri
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class PokemonsApi(
    private val httpClient: OkHttpClient
) {
    fun pokemonNames(
        offset: Int,
        limit: Int
    ): NetworkResponse<List<String>> {
        val response = httpClient.newCall(
            Request.Builder()
                .url("https://pokeapi.co/api/v2/pokemon?offset=$offset&limit=$limit")
                .build()
        ).execute()

        return NetworkResponse(
            httpResponse = response,
            data = response.body?.let {
                val json = JSONObject(it.string())
                val resultsJson = json.getJSONArray("results")
                List(resultsJson.length()) {
                    resultsJson.getJSONObject(it).getString("name")
                }
            }
        )
    }

    fun pokemon(name: String): NetworkResponse<Pokemon> {
        val response = httpClient.newCall(
            Request.Builder()
                .url("https://pokeapi.co/api/v2/pokemon/$name")
                .build()
        ).execute()

        return NetworkResponse(
            httpResponse = response,
            data = response.body?.let {
                val json = JSONObject(it.string())
                val spritesJson = json.getJSONObject("sprites")

                Pokemon(
                    name = name,
                    sprites = Pokemon.Sprites(
                        frontDefault = spritesJson.getString("front_default").toUri()
                    )
                )
            }
        )
    }

    data class Pokemon(
        val name: String,
        val sprites: Sprites
    ) {
        data class Sprites(
            val frontDefault: Uri
        )
    }
}