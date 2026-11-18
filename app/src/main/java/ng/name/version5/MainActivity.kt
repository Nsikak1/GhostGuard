package ng.name.version5

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ng.name.version5.ui.HomeScreen
import ng.name.version5.ui.ProfilesScreen
import ng.name.version5.ui.SettingsScreen
import ng.name.version5.ui.theme.Version5Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Version5Theme { GhostGuardApp() } }
    }
}

@Composable
fun GhostGuardApp() {
    var dest by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    // Bumped on every resume so permission/status rows re-read after returning from system screens.
    var refresh by remember { mutableIntStateOf(0) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val o = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(o); onDispose { owner.lifecycle.removeObserver(o) }
    }
    val notif = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notif.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    NavigationSuiteScaffold(navigationSuiteItems = {
        AppDestinations.entries.forEach {
            item(
                icon = { Icon(painterResource(it.icon), contentDescription = it.label) },
                label = { Text(it.label) },
                selected = it == dest,
                onClick = { dest = it }
            )
        }
    }) {
        Scaffold(modifier = Modifier.fillMaxSize()) { pad ->
            androidx.compose.foundation.layout.Box(Modifier.padding(pad)) {
                when (dest) {
                    AppDestinations.HOME -> HomeScreen(refresh)
                    AppDestinations.PROFILES -> ProfilesScreen()
                    AppDestinations.SETTINGS -> SettingsScreen(refresh)
                }
            }
        }
    }
}

enum class AppDestinations(val label: String, val icon: Int) {
    HOME("Protect", R.drawable.ic_home),
    PROFILES("Profiles", R.drawable.ic_favorite),
    SETTINGS("Settings", R.drawable.ic_account_box),
}
