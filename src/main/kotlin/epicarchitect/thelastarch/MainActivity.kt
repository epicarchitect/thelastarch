package epicarchitect.thelastarch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import epicarchitect.recyclerview.EpicAdapter
import epicarchitect.recyclerview.bind
import epicarchitect.recyclerview.requireEpicAdapter
import epicarchitect.thelastarch.databinding.ErrorItemBinding
import epicarchitect.thelastarch.databinding.LoadingItemBinding
import epicarchitect.thelastarch.databinding.PokemonItemBinding
import epicarchitect.thelastarch.tools.ErrorItem
import epicarchitect.thelastarch.tools.LoadingItem
import epicarchitect.thelastarch.tools.PagingData
import epicarchitect.thelastarch.tools.Worker
import epicarchitect.thelastarch.tools.appendPagingData
import epicarchitect.thelastarch.tools.createLoadingPagingData
import epicarchitect.thelastarch.tools.onScrolledToEnd
import epicarchitect.thelastarch.tools.toEpicItems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

val MainWorker = Worker(name = "MainActivity")
val AppendNextPokemonPageKey = Worker.Key<PagingData<PokemonsApi.Pokemon>>()
const val PokemonsPageSize = 20

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val recyclerView = RecyclerView(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        setContentView(recyclerView)

        recyclerView.onScrolledToEnd {
            val state = MainWorker.state(AppendNextPokemonPageKey).value ?: return@onScrolledToEnd
            if (state.error == null && !state.loading) {
                appendNextPokemonPage()
            }
        }

        recyclerView.adapter = createPokemonsAdapter(lifecycleScope)

        MainWorker.state(AppendNextPokemonPageKey).onEach { data ->
            if (data == null) {
                appendNextPokemonPage()
                return@onEach
            }

            recyclerView.requireEpicAdapter().loadItems(data.toEpicItems())
        }.launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        MainWorker.cancel(AppendNextPokemonPageKey)
    }
}

suspend fun requestNextPokemons(
    previousData: PagingData<PokemonsApi.Pokemon>?
) = withContext(Dispatchers.IO) {
    appendPagingData(previousData) {
        val names = Network.pokemons.pokemonNames(
            offset = PokemonsPageSize,
            limit = PokemonsPageSize
        ).data ?: emptyList()

        awaitAll(
            *names.map {
                async {
                    Network.pokemons.pokemon(it)
                }
            }.toTypedArray()
        ).mapNotNull {
            it.data
        }
    }
}

fun appendNextPokemonPage() {
    MainWorker.execute(
        key = AppendNextPokemonPageKey,
        executionConflictPolicy = Worker.ConflictPolicy.SKIP
    ) {
        emit(createLoadingPagingData(it?.items.orEmpty()))
        delay(3000) // simulate loading
        emit(requestNextPokemons(previousData = it))
    }
}

fun createPokemonsAdapter(coroutineScope: CoroutineScope) = EpicAdapter(coroutineScope) {
    setup<PokemonsApi.Pokemon, PokemonItemBinding>(PokemonItemBinding::inflate) {
        bind { item ->
            nameTextView.text = item.name
            Glide.with(avatarImageView)
                .load(item.sprites.frontDefault)
                .into(avatarImageView)
        }
    }

    setup<ErrorItem, ErrorItemBinding>(ErrorItemBinding::inflate) {
        init {
            retryButton.setOnClickListener {
                appendNextPokemonPage()
            }
        }
        bind { item ->
            errorTextView.text = item.text
        }
    }

    setup<LoadingItem, LoadingItemBinding>(LoadingItemBinding::inflate)
}