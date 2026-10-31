package dev.esmanhoto.leantv

import android.content.Context
import android.content.Intent
import android.media.tv.TvContract
import android.media.tv.TvInputManager

/** HDMI/passthrough inputs: listing with live-signal markers, and switching. */
object TvInputs {

    data class Entry(val id: String, val label: String)

    fun list(context: Context): List<Entry> {
        val manager = context.getSystemService(Context.TV_INPUT_SERVICE) as TvInputManager
        return manager.tvInputList.filter { it.isPassthroughInput }.map { input ->
            val label = input.loadLabel(context)?.toString()?.takeIf { it.isNotBlank() }
                ?: input.id.substringAfterLast('/')
            val live = manager.getInputState(input.id) == TvInputManager.INPUT_STATE_CONNECTED
            Entry(input.id, if (live) "$label  ●" else label)
        }
    }

    fun switchIntent(id: String): Intent =
        Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(id))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
