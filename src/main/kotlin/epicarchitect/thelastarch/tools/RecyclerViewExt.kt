package epicarchitect.thelastarch.tools

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.OnScrollListener
import epicarchitect.thelastarch.AppendNextPokemonPageKey
import epicarchitect.thelastarch.MainWorker

fun RecyclerView.onScrolledToEnd(action: () -> Unit) {
    addOnScrollListener(
        object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val state = MainWorker.state(AppendNextPokemonPageKey).value ?: return
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val itemCount = recyclerView.adapter?.itemCount ?: return
                val lastPosition = layoutManager.findLastCompletelyVisibleItemPosition()

                if (lastPosition >= itemCount - 1) {
                    action()
                }
            }
        }
    )
}