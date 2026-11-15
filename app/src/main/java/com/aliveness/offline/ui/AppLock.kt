package com.aliveness.offline.ui

import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aliveness.offline.MainViewModel
import com.aliveness.offline.data.Keys
import com.aliveness.offline.ui.components.AppLogo
import com.aliveness.offline.ui.components.findActivity
import com.aliveness.offline.ui.screens.SwitchRow
import com.aliveness.offline.ui.theme.Dimens

/** Fingerprint (or face / screen lock on Android 11+) through the system BiometricPrompt. */
object Biometrics {
    private val authenticators =
        if (Build.VERSION.SDK_INT >= 30) BIOMETRIC_STRONG or DEVICE_CREDENTIAL else BIOMETRIC_WEAK

    fun available(activity: FragmentActivity): Boolean =
        BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    fun prompt(activity: FragmentActivity, title: String, onResult: (Boolean) -> Unit) {
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onResult(false)
        }
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle("Aliveness")
            .setAllowedAuthenticators(authenticators)
            .apply { if (Build.VERSION.SDK_INT < 30) setNegativeButtonText("Cancel") }
            .build()
        BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback).authenticate(info)
    }
}

@Composable
fun LockScreen(onUnlocked: () -> Unit) {
    val activity = LocalContext.current.findActivity() as? FragmentActivity
    var failed by remember { mutableStateOf(false) }
    fun ask() {
        if (activity == null) return
        Biometrics.prompt(activity, "Unlock Aliveness") { ok -> if (ok) onUnlocked() else failed = true }
    }
    LaunchedEffect(Unit) { ask() }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(96.dp)
        Spacer(Modifier.height(18.dp))
        Text("Aliveness", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            if (failed) "Couldn’t verify. Try again." else "Locked",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))
        Button(onClick = { failed = false; ask() }) {
            Icon(Icons.Rounded.Fingerprint, null)
            Text("  Unlock")
        }
    }
}

/** Turning the lock on needs one successful check, so you can't lock yourself out by accident. */
@Composable
fun AppLockRow(vm: MainViewModel, enabled: Boolean) {
    val activity = LocalContext.current.findActivity() as? FragmentActivity
    var message by remember { mutableStateOf<String?>(null) }

    SwitchRow(
        title = "App lock",
        subtitle = message ?: "Ask for fingerprint or screen lock every time Aliveness opens",
        checked = enabled,
    ) { want ->
        message = null
        if (!want) {
            vm.setSetting(Keys.appLock, false)
        } else if (activity == null || !Biometrics.available(activity)) {
            message = "Set up a fingerprint or screen lock in your phone’s settings first."
        } else {
            Biometrics.prompt(activity, "Confirm to turn on app lock") { ok ->
                if (ok) vm.setSetting(Keys.appLock, true) else message = "Not turned on."
            }
        }
    }
}
