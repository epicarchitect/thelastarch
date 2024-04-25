package epicarchitect.thelastarch.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import com.bumptech.glide.Glide
import epicarchitect.recyclerview.EpicAdapter
import epicarchitect.recyclerview.bind
import epicarchitect.recyclerview.requireEpicAdapter
import epicarchitect.thelastarch.Worker
import epicarchitect.thelastarch.data.remote.Network
import epicarchitect.thelastarch.data.remote.PokemonsApi
import epicarchitect.thelastarch.databinding.PokemonItemBinding
import epicarchitect.thelastarch.databinding.PokemonsFragmentBinding
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

private val AppendNextPokemonPageKey = Worker.Key<AppendPokemonPage.Data>(
    conflictPolicy = Worker.ConflictPolicy.SKIP
)

class PokemonsFragment : Fragment() {

    private var _binding: PokemonsFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return PokemonsFragmentBinding.inflate(inflater, container, false).also {
            _binding = it
        }.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.pokemonsRecyclerView.addOnScrollListener(
            object : OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    if (recyclerView.computeVerticalScrollOffset() > recyclerView.height) {
                        appendNextPokemonPage()
                    }
                }
            }
        )

        binding.pokemonsRecyclerView.adapter = EpicAdapter {
            setup<PokemonsApi.Pokemon, PokemonItemBinding>(PokemonItemBinding::inflate) {
                bind { item ->
                    nameTextView.text = item.name
                    Glide.with(avatarImageView)
                        .load(item.sprites.frontDefault)
                        .into(avatarImageView)
                }
            }
        }

        Worker.global.state(AppendNextPokemonPageKey).onEach {
            if (it == null) {
                appendNextPokemonPage()
            }

            binding.pokemonsRecyclerView.isVisible = it != null
            binding.progressBar.isVisible = it == null
            binding.pokemonsRecyclerView.requireEpicAdapter().loadItems(it?.pokemons ?: emptyList())
        }.launchIn(viewLifecycleOwner.lifecycleScope)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        Worker.global.cancel(AppendNextPokemonPageKey)
    }

    private fun appendNextPokemonPage() {
        Worker.global.execute(AppendNextPokemonPageKey) {
            emit(
                AppendPokemonPage(
                    pokemonsApi = Network.pokemons,
                    pageSize = 20,
                    previousData = it
                ).data()
            )
        }
    }
}