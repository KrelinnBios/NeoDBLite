package com.krelinnbios.neodblite

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.krelinnbios.neodblite.data.model.ItemBrief
import com.krelinnbios.neodblite.data.model.ShelfType
import com.krelinnbios.neodblite.global.OAuthBus
import com.krelinnbios.neodblite.ui.component.AppUpdateDialog
import com.krelinnbios.neodblite.ui.component.LoadingBox
import com.krelinnbios.neodblite.ui.page.DiscoverPage
import com.krelinnbios.neodblite.ui.page.ItemDetailPage
import com.krelinnbios.neodblite.ui.page.LoginPage
import com.krelinnbios.neodblite.ui.page.ProfilePage
import com.krelinnbios.neodblite.ui.page.SearchPage
import com.krelinnbios.neodblite.ui.page.SettingsPage
import com.krelinnbios.neodblite.ui.page.ShelfPage
import com.krelinnbios.neodblite.ui.theme.NeoDBLiteTheme
import com.krelinnbios.neodblite.ui.vm.AuthState
import com.krelinnbios.neodblite.ui.vm.AuthViewModel
import com.krelinnbios.neodblite.ui.vm.DetailViewModel
import com.krelinnbios.neodblite.ui.vm.DiscoverViewModel
import com.krelinnbios.neodblite.ui.vm.ProfileViewModel
import com.krelinnbios.neodblite.ui.vm.SearchViewModel
import com.krelinnbios.neodblite.ui.vm.ShelfViewModel
import com.krelinnbios.neodblite.util.AppUpdateCheckResult
import com.krelinnbios.neodblite.util.AppUpdateInfo
import com.krelinnbios.neodblite.util.AppUpdateManager
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleOAuthRedirect(intent)
        setContent {
            NeoDBLiteTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NeoDBLiteApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthRedirect(intent)
    }

    /** 解析 neodblite://oauth/callback?code=... 中的授权 code。 */
    private fun handleOAuthRedirect(intent: Intent?) {
        val data: Uri = intent?.data ?: return
        if (data.scheme != "neodblite") return
        val code = data.getQueryParameter("code") ?: return
        OAuthBus.pendingCode.value = code
    }
}

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomDestinations = listOf(
    BottomDestination("discover", "发现", Icons.Filled.Home),
    BottomDestination("search", "搜索", Icons.Filled.Search),
    BottomDestination("shelf", "书架", Icons.AutoMirrored.Filled.List),
    BottomDestination("profile", "我的", Icons.Filled.Person),
    BottomDestination("settings", "设置", Icons.Filled.Settings)
)

@Composable
private fun NeoDBLiteApp() {
    val authVM: AuthViewModel = viewModel()
    val authState by authVM.authState.collectAsStateWithLifecycle()

    // 消费 OAuth 回调 code。
    val pendingCode by OAuthBus.pendingCode.collectAsStateWithLifecycle()
    LaunchedEffect(pendingCode) {
        pendingCode?.let {
            authVM.handleAuthCode(it)
            OAuthBus.pendingCode.value = null
        }
    }

    when (val state = authState) {
        is AuthState.Loading -> LoadingBox()
        is AuthState.LoggedOut -> LoginPage(authVM)
        is AuthState.LoggedIn -> MainScaffold(authVM = authVM, userName = state.user)
    }
}

@Composable
private fun MainScaffold(
    authVM: AuthViewModel,
    userName: com.krelinnbios.neodblite.data.model.NeoUser
) {
    val navController = rememberNavController()
    val discoverVM: DiscoverViewModel = viewModel()
    val searchVM: SearchViewModel = viewModel()
    val shelfVM: ShelfViewModel = viewModel()
    val profileVM: ProfileViewModel = viewModel()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomDestinations.map { it.route }

    // 启动时静默检查更新（带节流）。
    var updateInfo by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<AppUpdateInfo?>(null)
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        when (val result = AppUpdateManager.checkForUpdateAuto(context)) {
            is AppUpdateCheckResult.UpdateAvailable -> updateInfo = result.info
            else -> Unit
        }
    }

    val openItem: (ItemBrief) -> Unit = { item ->
        val path = item.fetchPath
        if (!path.isNullOrBlank()) {
            val encoded = URLEncoder.encode(path, "utf-8")
            navController.navigate("detail?path=$encoded")
        }
    }

    // 从个人主页的统计卡片跳到书架，并选中对应状态。
    val openShelf: (ShelfType) -> Unit = { type ->
        shelfVM.selectShelf(type)
        if (currentRoute != "shelf") {
            navController.navigate("shelf") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomDestinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route,
                            onClick = {
                                if (currentRoute != dest.route) {
                                    navController.navigate(dest.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "discover",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            composable("discover") {
                DiscoverPage(discoverVM = discoverVM, onOpenItem = openItem)
            }
            composable("search") {
                SearchPage(searchVM = searchVM, onOpenItem = openItem)
            }
            composable("shelf") {
                ShelfPage(shelfVM = shelfVM, onOpenItem = openItem)
            }
            composable("profile") {
                ProfilePage(
                    user = userName,
                    host = authVM.currentHost,
                    profileVM = profileVM,
                    onOpenItem = openItem,
                    onOpenShelf = openShelf
                )
            }
            composable("settings") {
                SettingsPage(
                    user = userName,
                    host = authVM.currentHost,
                    onLogout = { authVM.logout() }
                )
            }
            composable(
                route = "detail?path={path}",
                arguments = listOf(navArgument("path") { type = NavType.StringType; defaultValue = "" })
            ) { entry ->
                val encoded = entry.arguments?.getString("path").orEmpty()
                val path = if (encoded.isBlank()) "" else URLDecoder.decode(encoded, "utf-8")
                val detailVM: DetailViewModel = viewModel()
                ItemDetailPage(
                    path = path,
                    detailVM = detailVM,
                    onBack = { navController.popBackStack() },
                    onSearchTag = { tag ->
                        searchVM.onQueryChange(tag)
                        searchVM.submit()
                        navController.navigate("search") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }

    updateInfo?.let { info ->
        AppUpdateDialog(info = info, onDismiss = { updateInfo = null })
    }
}
