package dev.esmanhoto.leantv

import android.accessibilityservice.AccessibilityService
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

/**
 * Remaps branded remote buttons that the OS delivers to the accessibility key
 * filter. Must be enabled by hand: Settings > Accessibility > Lean TV key remap.
 */
class KeyRemapService : AccessibilityService() {

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_TV_INPUT) {
            if (event.action == KeyEvent.ACTION_DOWN) showInputsOverlay()
            return true
        }
        val target = REMAP[event.keyCode] ?: return false
        if (event.action == KeyEvent.ACTION_DOWN) {
            val intent = packageManager.getLeanbackLaunchIntentForPackage(target)
                ?: packageManager.getLaunchIntentForPackage(target)
                ?: return false
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "cannot launch $target", e)
            }
        }
        return true
    }

    /** Input picker over whatever is on screen; dismissing returns to it untouched. */
    private fun showInputsOverlay() {
        val inputs = TvInputs.list(this)
        if (inputs.isEmpty()) return
        val themed = ContextThemeWrapper(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
        val dialog = AlertDialog.Builder(themed)
            .setTitle(R.string.inputs)
            .setItems(inputs.map { it.label }.toTypedArray()) { _, which ->
                try {
                    startActivity(TvInputs.switchIntent(inputs[which].id))
                } catch (e: Exception) {
                    Log.w(TAG, "cannot switch input", e)
                }
            }
            .create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY)
        dialog.show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() = Unit

    private companion object {
        const val TAG = "LeanKeyRemap"
        // Prime Video button (194) -> HBO Max
        val REMAP = mapOf(194 to "com.wbd.stream")
    }
}
