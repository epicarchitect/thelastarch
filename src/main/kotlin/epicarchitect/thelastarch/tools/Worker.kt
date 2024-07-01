@file:Suppress("UNCHECKED_CAST")

package epicarchitect.thelastarch.tools

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Worker(val name: String) {
    val resultStateMap = mutableMapOf<Any, Work>()

    inline fun <reified T> execute(
        key: Key<T>,
        executionConflictPolicy: ConflictPolicy = ConflictPolicy.APPEND,
        noinline action: suspend FlowCollector<T>.(lastValue: T?) -> Unit
    ) {
        Log.d("Worker-$name", "$key -> execute")
        val work = resultStateMap.getOrPut(key) { Work() }
        if (executionConflictPolicy == ConflictPolicy.SKIP &&
            work.coroutineScope.coroutineContext.job.children.any { it.isActive }
        ) {
            Log.d("Worker-$name", "$key -> skip")
            return
        }

        if (executionConflictPolicy == ConflictPolicy.REPLACE) {
            Log.d("Worker-$name", "$key -> replace")
            work.coroutineScope.coroutineContext.cancelChildren()
        }

        work.coroutineScope.launch {
            Log.d("Worker-$name", "$key -> append")
            work.mutex.withLock {
                Log.d("Worker-$name", "$key -> execute")
                val lastValue = work.state.value
                flow {
                    action(lastValue as? T)
                }.collect(work.state)
                Log.d("Worker-$name", "$key -> executed")
            }
        }
    }

    inline fun <reified T> state(
        key: Key<T>
    ) = resultStateMap.getOrPut(key) { Work() }.state as StateFlow<T?>

    fun cancel(key: Any) {
        val entry = resultStateMap.remove(key) ?: return
        entry.state.value = null
        entry.coroutineScope.cancel()
        Log.d("Worker-$name", "$key -> canceled")
    }

    class Work {
        val coroutineScope = CoroutineScope(Dispatchers.Default)
        val mutex: Mutex = Mutex()
        val state = MutableStateFlow<Any?>(null)
    }

    enum class ConflictPolicy {
        REPLACE,
        SKIP,
        APPEND,
    }

    class Key<T>
}