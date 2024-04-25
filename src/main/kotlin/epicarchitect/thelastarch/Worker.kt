@file:Suppress("UNCHECKED_CAST")

package epicarchitect.thelastarch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Worker {
    val coroutineScope = CoroutineScope(Dispatchers.Default)
    val resultStateMap = mutableMapOf<Any, Work>()

    inline fun <reified T> execute(
        key: WorkerKey<T>,
        executionConflictPolicy: ExecutionConflictPolicy = key.defaultExecutionConflictPolicy,
        noinline action: suspend FlowCollector<T>.(lastValue: T?) -> Unit
    ) {
        val work = resultStateMap.getOrPut(key) { Work() }
        if (executionConflictPolicy == ExecutionConflictPolicy.SKIP && work.job?.isActive == true) {
            return
        }

        if (executionConflictPolicy == ExecutionConflictPolicy.REPLACE) {
            work.job?.cancel()
            work.job = null
        }

        work.job = coroutineScope.launch {
            work.mutex.withLock {
                val lastValue = work.state.value
                flow {
                    action(lastValue as? T)
                }.collect {
                    work.state.value = it
                }
            }
        }
    }

    inline fun <reified T> state(
        key: WorkerKey<T>
    ) = resultStateMap.getOrPut(key) { Work() }.state as StateFlow<T?>

    fun cancel(key: Any) {
        val entry = resultStateMap.remove(key) ?: return
        entry.state.value = null
        entry.job?.cancel()
        entry.job = null
    }

    class Work {
        var job: Job? = null
        val mutex: Mutex = Mutex()
        val state = MutableStateFlow<Any?>(null)
    }

    enum class ExecutionConflictPolicy {
        REPLACE,
        SKIP,
        APPEND,
    }

    companion object {
        val global = Worker()
    }
}

class WorkerKey<T>(
    val defaultExecutionConflictPolicy: Worker.ExecutionConflictPolicy
)

inline fun <reified T> workerKey(
    defaultExecutionConflictPolicy: Worker.ExecutionConflictPolicy = Worker.ExecutionConflictPolicy.APPEND
) = WorkerKey<T>(defaultExecutionConflictPolicy)