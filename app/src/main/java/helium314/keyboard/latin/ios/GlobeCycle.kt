// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.ios

/**
 * Which language the globe switches to: a single tap returns to the previously used language, taps in quick
 * succession (within [windowMs] of each other) go on through all languages from the most recently used, like
 * Alt-Tab. Languages not used yet follow in list order.
 */
class GlobeCycle<T> @JvmOverloads constructor(private val windowMs: Long = 1000) {
    /** most recently used first */
    private val recent = mutableListOf<T>()
    private var burst: List<T> = emptyList()
    private var burstIndex = 0
    private var lastTap = Long.MIN_VALUE

    /** Call whenever a language becomes current, however it was chosen. */
    fun onShown(item: T) {
        recent.remove(item)
        recent.add(0, item)
    }

    /** The language a globe tap at [now] (ms) switches to, or null if there is no other one. */
    fun next(current: T, enabled: List<T>, now: Long): T? {
        if (enabled.size < 2) return null
        val quick = lastTap != Long.MIN_VALUE && now - lastTap <= windowMs
        lastTap = now
        if (quick && burst.size > 1 && burst.toSet() == enabled.toSet() && burst[burstIndex] == current) {
            burstIndex = (burstIndex + 1) % burst.size
        } else {
            onShown(current)
            recent.retainAll(enabled.toSet())
            enabled.forEach { if (it !in recent) recent.add(it) }
            burst = recent.toList()
            // a current language that is not enabled any more is not in the list: go to the first one
            burstIndex = if (current in enabled) 1 else 0
        }
        val target = burst[burstIndex]
        // as if the cycle stopped here: the language the cycle started from is the one to go back to
        val origin = burst[0]
        onShown(origin)
        if (target != origin) onShown(target)
        return target
    }
}
