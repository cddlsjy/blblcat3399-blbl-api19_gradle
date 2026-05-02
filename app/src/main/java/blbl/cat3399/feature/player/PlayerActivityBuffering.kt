package blbl.cat3399.feature.player

import android.os.SystemClock
import android.view.View
import androidx.media3.common.Player
import blbl.cat3399.R

private const val BUFFERING_SPEED_WINDOW_MS = 800L
private const val BUFFERING_SPEED_STALE_MS = 2_000L
private const val BUFFERING_OVERLAY_SHOW_DELAY_MS = 1_000L

internal class BufferingSpeedMeter {
    @Volatile private var lastBytesPerSecond: Long? = null
    @Volatile private var lastUpdatedAtMs: Long = 0L

    private var sampleBytes: Long = 0L
    private var sampleStartAtMs: Long = 0L

    @Synchronized
    fun reset() {
        sampleBytes = 0L
        sampleStartAtMs = 0L
        lastBytesPerSecond = null
        lastUpdatedAtMs = 0L
    }

    @Synchronized
    fun addBytes(bytes: Long, nowMs: Long = SystemClock.elapsedRealtime()) {
        if (bytes <= 0L) return
        if (sampleStartAtMs <= 0L) {
            sampleStartAtMs = nowMs
        }
        sampleBytes += bytes
        val elapsedMs = nowMs - sampleStartAtMs
        if (elapsedMs < BUFFERING_SPEED_WINDOW_MS) return
        lastBytesPerSecond = (sampleBytes * 1000L / elapsedMs.coerceAtLeast(1L)).coerceAtLeast(0L)
        lastUpdatedAtMs = nowMs
        sampleBytes = 0L
        sampleStartAtMs = nowMs
    }

    fun currentBytesPerSecond(nowMs: Long = SystemClock.elapsedRealtime()): Long? {
        val speed = lastBytesPerSecond ?: return null
        return speed.takeIf { nowMs - lastUpdatedAtMs <= BUFFERING_SPEED_STALE_MS }
    }
}

internal fun PlayerActivity.recordBufferingTransferBytes(bytes: Long) {
    if (!bufferingSpeedTrackingEnabled) return
    bufferingSpeedMeter.addBytes(bytes)
}

internal fun PlayerActivity.resetBufferingOverlayState() {
    bufferingStateStartedAtMs = 0L
    bufferingSpeedTrackingEnabled = false
    bufferingSpeedMeter.reset()
    keySeekBufferingOverlaySuppressedUntilMs = 0L
    keySeekBufferingOverlayEligibleAtMs = 0L
    binding.bufferingOverlay.visibility = View.GONE
    binding.tvBuffering.text = getString(R.string.player_loading)
}

internal fun PlayerActivity.suppressBufferingOverlayDuringKeySeek(commitDelayMs: Long) {
    val nowMs = SystemClock.elapsedRealtime()
    val suppressedUntil = nowMs + commitDelayMs.coerceAtLeast(0L) + PlayerActivity.KEY_SEEK_BUFFERING_POST_COMMIT_GRACE_MS
    if (suppressedUntil > keySeekBufferingOverlaySuppressedUntilMs) {
        keySeekBufferingOverlaySuppressedUntilMs = suppressedUntil
    }
    if (keySeekBufferingOverlayEligibleAtMs < keySeekBufferingOverlaySuppressedUntilMs) {
        keySeekBufferingOverlayEligibleAtMs = keySeekBufferingOverlaySuppressedUntilMs
    }
}

internal fun PlayerActivity.clearKeySeekBufferingOverlaySuppression() {
    keySeekBufferingOverlaySuppressedUntilMs = 0L
    keySeekBufferingOverlayEligibleAtMs = 0L
}

internal fun PlayerActivity.updateBufferingOverlay() {
    val nowMs = SystemClock.elapsedRealtime()
    val isBuffering = player?.playbackState == Player.STATE_BUFFERING
    val suppressedUntil = keySeekBufferingOverlaySuppressedUntilMs
    if (suppressedUntil > 0L && nowMs < suppressedUntil) {
        if (binding.bufferingOverlay.visibility != View.GONE) {
            binding.bufferingOverlay.visibility = View.GONE
        }
        binding.tvBuffering.text = getString(R.string.player_loading)
        if (isBuffering && keySeekBufferingOverlayEligibleAtMs < suppressedUntil) {
            keySeekBufferingOverlayEligibleAtMs = suppressedUntil
        }
        return
    }

    if (!isBuffering) {
        clearKeySeekBufferingOverlaySuppression()
        if (binding.bufferingOverlay.visibility != View.GONE) {
            binding.bufferingOverlay.visibility = View.GONE
        }
        binding.tvBuffering.text = getString(R.string.player_loading)
        return
    }

    if (suppressedUntil > 0L) {
        keySeekBufferingOverlaySuppressedUntilMs = 0L
    }

    val bufferingStartedAtMs =
        maxOf(
            bufferingStateStartedAtMs,
            keySeekBufferingOverlayEligibleAtMs.takeIf { it > 0L } ?: 0L,
        )
    if (bufferingStartedAtMs <= 0L || nowMs - bufferingStartedAtMs < BUFFERING_OVERLAY_SHOW_DELAY_MS) {
        if (binding.bufferingOverlay.visibility != View.GONE) {
            binding.bufferingOverlay.visibility = View.GONE
        }
        binding.tvBuffering.text = getString(R.string.player_loading)
        return
    }

    val text =
        bufferingSpeedMeter
            .currentBytesPerSecond(nowMs)
            ?.takeIf { it > 0L }
            ?.let { getString(R.string.player_loading_speed, formatTransferBytes(it)) }
            ?: getString(R.string.player_loading)

    binding.tvBuffering.text = text
    if (binding.bufferingOverlay.visibility != View.VISIBLE) {
        binding.bufferingOverlay.visibility = View.VISIBLE
    }
}
