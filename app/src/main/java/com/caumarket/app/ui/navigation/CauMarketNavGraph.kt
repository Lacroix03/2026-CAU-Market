package com.caumarket.app.ui.navigation

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.caumarket.app.di.AppContainer
import com.caumarket.app.ui.auth.AuthViewModel
import com.caumarket.app.ui.auth.LoginScreen
import com.caumarket.app.ui.auth.SignUpScreen
import com.caumarket.app.ui.chat.ChatListScreen
import com.caumarket.app.ui.chat.ChatListViewModel
import com.caumarket.app.ui.chat.ChatRoomScreen
import com.caumarket.app.ui.chat.ChatRoomViewModel
import com.caumarket.app.ui.mypage.MyPageScreen
import com.caumarket.app.ui.mypage.MyPageViewModel
import com.caumarket.app.ui.product.ProductDetailScreen
import com.caumarket.app.ui.product.ProductDetailViewModel
import com.caumarket.app.ui.product.ProductFormScreen
import com.caumarket.app.ui.product.ProductFormViewModel
import com.caumarket.app.ui.product.ProductListScreen
import com.caumarket.app.ui.product.ProductListViewModel
import com.caumarket.app.ui.theme.BorderGray
import com.caumarket.app.ui.theme.PrimaryBlue
import com.caumarket.app.ui.theme.TextHint
import com.caumarket.app.ui.theme.UnreadRed

object Routes {
    const val LOGIN = "login"
    const val SIGN_UP = "signup"
    const val PRODUCT_LIST = "products"
    const val PRODUCT_DETAIL = "products/{productId}"
    const val PRODUCT_CREATE = "product_create"
    const val PRODUCT_EDIT = "product_edit/{productId}"
    const val CHAT_LIST = "chats"
    const val CHAT_ROOM = "chats/{chatId}"
    const val MY_PAGE = "mypage"

    fun productDetail(productId: String) = "products/$productId"
    fun productEdit(productId: String) = "product_edit/$productId"
    fun chatRoom(chatId: String) = "chats/$chatId"
}

private enum class MainTab(val route: String, val label: String, val icon: ImageVector) {
    PRODUCTS(Routes.PRODUCT_LIST, "물품 목록", Icons.Default.Menu),
    CHATS(Routes.CHAT_LIST, "채팅", Icons.Outlined.ChatBubbleOutline),
    MY_PAGE(Routes.MY_PAGE, "마이페이지", Icons.Outlined.PersonOutline)
}

@Composable
fun CauMarketApp(container: AppContainer) {
    // Android 17(API 37+) 로컬 네트워크 권한 요청
    val localNetworkPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 37) {
            localNetworkPermissionLauncher.launch("android.permission.ACCESS_LOCAL_NETWORK")
        }
    }

    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.authRepository))
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val isCheckingSession by authViewModel.isCheckingSession.collectAsStateWithLifecycle()

    val chatListViewModel: ChatListViewModel = viewModel(
        factory = ChatListViewModel.factory(container.authRepository, container.chatRepository)
    )
    val chatListState by chatListViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentUser?.userId) {
        if (currentUser != null) chatListViewModel.startObserving()
        else chatListViewModel.stopObserving()
    }

    if (isCheckingSession) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = PrimaryBlue)
        }
        return
    }

    val bottomBar: @Composable () -> Unit = {
        FigmaBottomBar(navController = navController, totalUnreadChats = chatListState.totalUnreadCount)
    }

    val startDestination = if (currentUser != null) Routes.PRODUCT_LIST else Routes.LOGIN

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.PRODUCT_LIST) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Routes.SIGN_UP) }
            )
        }

        composable(Routes.SIGN_UP) {
            SignUpScreen(
                viewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onSignUpSuccess = { navController.popBackStack() }
            )
        }

        composable(Routes.PRODUCT_LIST) {
            val vm: ProductListViewModel = viewModel(factory = ProductListViewModel.factory(container.productRepository))
            ProductListScreen(
                viewModel = vm,
                onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                onCreateProductClick = { navController.navigate(Routes.PRODUCT_CREATE) },
                bottomBar = bottomBar
            )
        }

        composable(
            route = Routes.PRODUCT_DETAIL,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { entry ->
            val productId = entry.arguments?.getString("productId").orEmpty()
            val vm: ProductDetailViewModel = viewModel(
                key = "detail_$productId",
                factory = ProductDetailViewModel.factory(
                    productId = productId,
                    authRepository = container.authRepository,
                    productRepository = container.productRepository,
                    chatRepository = container.chatRepository
                )
            )
            ProductDetailScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onEditClick = { id -> navController.navigate(Routes.productEdit(id)) },
                onDeleted = { navController.popBackStack() },
                onOpenChat = { chatId -> navController.navigate(Routes.chatRoom(chatId)) }
            )
        }

        composable(Routes.PRODUCT_CREATE) {
            val vm: ProductFormViewModel = viewModel(
                factory = ProductFormViewModel.factory(null, container.productRepository)
            )
            ProductFormScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { newId ->
                    navController.navigate(Routes.productDetail(newId)) {
                        popUpTo(Routes.PRODUCT_CREATE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Routes.PRODUCT_EDIT,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { entry ->
            val productId = entry.arguments?.getString("productId").orEmpty()
            val vm: ProductFormViewModel = viewModel(
                key = "edit_$productId",
                factory = ProductFormViewModel.factory(productId, container.productRepository)
            )
            ProductFormScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    navController.navigate(Routes.productDetail(savedId)) {
                        popUpTo(Routes.PRODUCT_EDIT) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(Routes.CHAT_LIST) {
            ChatListScreen(
                viewModel = chatListViewModel,
                onChatClick = { chatId -> navController.navigate(Routes.chatRoom(chatId)) },
                bottomBar = bottomBar
            )
        }

        composable(
            route = Routes.CHAT_ROOM,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { entry ->
            val chatId = entry.arguments?.getString("chatId").orEmpty()
            val vm: ChatRoomViewModel = viewModel(
                key = "chat_$chatId",
                factory = ChatRoomViewModel.factory(
                    chatId = chatId,
                    authRepository = container.authRepository,
                    chatRepository = container.chatRepository
                )
            )
            ChatRoomScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onProductClick = { pid -> navController.navigate(Routes.productDetail(pid)) }
            )
        }

        composable(Routes.MY_PAGE) {
            val vm: MyPageViewModel = viewModel(
                factory = MyPageViewModel.factory(container.authRepository, container.productRepository)
            )
            MyPageScreen(
                viewModel = vm,
                onProductClick = { id -> navController.navigate(Routes.productDetail(id)) },
                onEditProductClick = { id -> navController.navigate(Routes.productEdit(id)) },
                onCreateProductClick = { navController.navigate(Routes.PRODUCT_CREATE) },
                onLogoutClick = {
                    authViewModel.logout {
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                },
                bottomBar = bottomBar
            )
        }
    }
}

@Composable
private fun FigmaBottomBar(
    navController: NavHostController,
    totalUnreadChats: Int
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Column {
        HorizontalDivider(color = BorderGray)
        NavigationBar(containerColor = Color.White) {
            MainTab.entries.forEach { tab ->
                val selected = currentRoute == tab.route
                NavigationBarItem(
                    selected = selected,
                    onClick = {
                        if (!selected) {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        unselectedIconColor = TextHint,
                        unselectedTextColor = TextHint,
                        indicatorColor = Color.Transparent
                    ),
                    icon = {
                        if (tab == MainTab.CHATS && totalUnreadChats > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = UnreadRed, contentColor = Color.White) {
                                        Text(if (totalUnreadChats >= 100) "99+" else totalUnreadChats.toString())
                                    }
                                }
                            ) { Icon(tab.icon, contentDescription = tab.label) }
                        } else {
                            Icon(tab.icon, contentDescription = tab.label)
                        }
                    },
                    label = { Text(tab.label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }
    }
}
