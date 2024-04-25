package epicarchitect.thelastarch.ui

import epicarchitect.thelastarch.data.remote.PokemonsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class AppendPokemonPage(
    private val pokemonsApi: PokemonsApi,
    private val pageSize: Int,
    private val previousData: Data?
) {

    suspend fun data() = withContext(Dispatchers.IO) {
        val offset = previousData?.offset?.plus(pageSize) ?: 0
        val lastList = previousData?.pokemons ?: emptyList()

        val names = pokemonsApi.pokemonNames(
            offset = offset,
            limit = pageSize
        ).data ?: emptyList()

        val pokemons = awaitAll(
            *names.map {
                async {
                    pokemonsApi.pokemon(it)
                }
            }.toTypedArray()
        ).mapNotNull {
            it.data
        }

        Data(
            pokemons = lastList + pokemons,
            offset = offset
        )
    }

    data class Data(
        val pokemons: List<PokemonsApi.Pokemon>,
        val offset: Int
    )
}