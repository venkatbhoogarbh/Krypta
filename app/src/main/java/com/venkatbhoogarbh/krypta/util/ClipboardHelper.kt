package com.venkatbhoogarbh.krypta.util

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import com.venkatbhoogarbh.krypta.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Utility to manage secure clipboard operations in Krypta.
 * Centralizes copying and automatic timed clearing with ownership verification.
 */
object ClipboardHelper {
    private const val KRYPTA_CLIP_LABEL = "Krypta Secure Data"
    private const val KRYPTA_TOKEN_KEY = "krypta_token"
    
    // Manage active clearing jobs to avoid multiple timers running simultaneously
    private var activeClearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     * Copies text to the clipboard and schedules an automatic clear after 30 seconds.
     * 
     * @param context Application context
     * @param text The sensitive text to copy
     * @param confirmationMessage The string resource for the "Copied" Toast
     */
    fun copyToClipboard(context: Context, text: String, confirmationMessage: Int) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return

        val newToken = UUID.randomUUID().toString()
        val clip = ClipData.newPlainText(KRYPTA_CLIP_LABEL, text)
        
        // Secure token-based ownership tracking using ClipDescription extras
        clip.description.extras = PersistableBundle().apply {
            putString(KRYPTA_TOKEN_KEY, newToken)
            // Prevent clipboard previews on Android 13+ (API 33)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, context.getString(confirmationMessage), Toast.LENGTH_SHORT).show()

        // Thread-safe timer management: Cancel any existing Krypta clearing job
        activeClearJob?.cancel()
        
        val appContext = context.applicationContext
        activeClearJob = scope.launch {
            delay(30000) // 30 seconds
            clearIfOwned(appContext, newToken)
        }
    }

    /**
     * Clears the clipboard only if it still contains the exact item copied by this specific operation.
     */
    private fun clearIfOwned(context: Context, operationToken: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return

        val primaryClip = clipboard.primaryClip
        
        // Ownership Verification: Label check + Unique Operation Token check
        if (primaryClip != null && primaryClip.description.label == KRYPTA_CLIP_LABEL) {
            val clipToken = primaryClip.description.extras?.getString(KRYPTA_TOKEN_KEY)
            
            if (clipToken == operationToken) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    clipboard.clearPrimaryClip()
                } else {
                    // Fallback for API < 28
                    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
                }
                Toast.makeText(context, context.getString(R.string.clipboard_cleared), Toast.LENGTH_SHORT).show()
            }
        }
    }
}
