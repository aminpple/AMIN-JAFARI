package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.ArticleEntity
import com.example.data.local.ChandelierEntity
import com.example.ui.NabloosViewModel
import com.example.ui.UiState
import com.example.ui.components.NabloosWebView
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // Clear WebView cache directory to resolve any permission/ownership conflicts
            // arising from repeated incremental installation/UID changes.
            val webViewCacheDir = java.io.File(cacheDir, "WebView")
            if (webViewCacheDir.exists()) {
                webViewCacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to clear WebView cache", e)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainScreen()
            }
        }
    }
}

// Bottom Navigation items definition
sealed class Screen(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("خانه", Icons.Default.Home)
    object Shop : Screen("فروشگاه لوستر", Icons.Default.ShoppingCart)
    object Journal : Screen("مجله نورپردازی", Icons.Default.Book)
    object Account : Screen("حساب کاربری", Icons.Default.Person)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: NabloosViewModel = viewModel(factory = NabloosViewModel.Factory)
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var webViewUrl by remember { mutableStateOf("https://nabloostr.com") }
    var reloadTrigger by remember { mutableStateOf(0) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    // Setup 100% Persian Right-to-Left (RTL) Layout Rule in Compose
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(300.dp),
                    drawerContainerColor = MaterialTheme.colorScheme.background
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = "لوگو ناب لوستر",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "صنایع لوستر و روشنایی ناب لوستر",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "طراحی و ساخت لوسترهای سفارشی مجلل",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("خانه اصلی") },
                        selected = currentScreen == Screen.Home && webViewUrl == "https://nabloostr.com",
                        onClick = {
                            currentScreen = Screen.Home
                            webViewUrl = "https://nabloostr.com"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
                        label = { Text("گالری لوسترها (آنلاین)") },
                        selected = currentScreen == Screen.Shop && webViewUrl == "https://nabloostr.com/shop",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/shop"
                            currentScreen = Screen.Shop
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ShoppingBag, contentDescription = null) },
                        label = { Text("سبد خرید") },
                        selected = webViewUrl == "https://nabloostr.com/cart",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/cart"
                            currentScreen = Screen.Shop
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                        label = { Text("لیست علاقه‌مندی‌ها") },
                        selected = webViewUrl == "https://nabloostr.com/wishlist",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/wishlist"
                            currentScreen = Screen.Shop
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Book, contentDescription = null) },
                        label = { Text("مجله نورپردازی (بلاگ)") },
                        selected = currentScreen == Screen.Journal && webViewUrl == "https://nabloostr.com/blog",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/blog"
                            currentScreen = Screen.Journal
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text("حساب کاربری") },
                        selected = currentScreen == Screen.Account && webViewUrl == "https://nabloostr.com/my-account",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/my-account"
                            currentScreen = Screen.Account
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ListAlt, contentDescription = null) },
                        label = { Text("پیگیری سفارش‌ها") },
                        selected = (webViewUrl == "https://nabloostr.com/track-order" || webViewUrl.contains("orders")),
                        onClick = {
                            webViewUrl = "https://nabloostr.com/my-account/orders"
                            currentScreen = Screen.Account
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.ContactSupport, contentDescription = null) },
                        label = { Text("تماس با ما / مشاوره") },
                        selected = webViewUrl == "https://nabloostr.com/contact",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/contact"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("درباره ناب لوستر") },
                        selected = webViewUrl == "https://nabloostr.com/about",
                        onClick = {
                            webViewUrl = "https://nabloostr.com/about"
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                        label = { Text("تماس با پشتیبانی تلفنی") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09112520113"))
                            context.startActivity(dialIntent)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                        label = { Text("آدرس کارگاه و نمایشگاه مرکزی") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:35.6892,51.3890?q=تهران لوستر ناب لوستر"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Text(
                        text = "نسخه ۱.۰.۰ - طراحی شده برای nabloostr.com",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        ) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ناب لوستر",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "LUXURY LIGHTING",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_drawer_button")
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "منو جانبی")
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { reloadTrigger++ },
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "بروزرسانی",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            navigationIconContentColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = Color.White,
                            actionIconContentColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                bottomBar = {
                    NavigationBar(
                        modifier = Modifier
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .testTag("bottom_navigation_bar"),
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 8.dp
                    ) {
                        val screens = listOf(
                            Screen.Home,
                            Screen.Shop,
                            Screen.Journal,
                            Screen.Account
                        )
                        screens.forEach { s ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        imageVector = s.icon,
                                        contentDescription = s.title,
                                        modifier = Modifier.testTag("nav_icon_${s.title}")
                                    )
                                },
                                label = {
                                    Text(
                                        text = s.title,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                selected = currentScreen == s,
                                onClick = { 
                                    currentScreen = s 
                                    webViewUrl = when (s) {
                                        Screen.Home -> "https://nabloostr.com"
                                        Screen.Shop -> "https://nabloostr.com/shop"
                                        Screen.Journal -> "https://nabloostr.com/blog"
                                        Screen.Account -> "https://nabloostr.com/my-account"
                                    }
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background)
                    ) {
                    NabloosBrowserScreen(
                        url = webViewUrl,
                        onUrlChanged = { webViewUrl = it },
                        reloadTrigger = reloadTrigger
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    viewModel: NabloosViewModel,
    onNavigateToShop: (String) -> Unit
) {
    val cachedChandeliers by viewModel.cachedChandeliers.collectAsStateWithLifecycle()
    val refreshState by viewModel.refreshState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Hero Visual Banner Section
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_luxury_chandelier_banner),
                    contentDescription = "لوستر لوکس ناب لوستر",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                            )
                        )
                )
                // Iranian Warm Glow overlay text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp)
                ) {
                    Text(
                        text = "شکوه نور در دکوراسیون کلاسیک و مدرن",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "نمایشگاه تخصصی لوستر و آویزهای کریستالی اورجینال",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Subtitle / Categories Row
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "دسته‌بندی‌های ممتاز",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "مشاهده وب‌سایت",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigateToShop("https://nabloostr.com/shop") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val categories = listOf(
                        "کریستال شامپاینی" to "https://nabloostr.com/product-category/crystal-chandelier/",
                        "مدرن سیلیکونی" to "https://nabloostr.com/product-category/modern-chandelier/",
                        "برنزی سلطنتی" to "https://nabloostr.com/product-category/bronze-chandelier/",
                        "دیوارکوب لوکس" to "https://nabloostr.com/product-category/wall-lamps/"
                    )
                    items(categories) { (name, link) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            onClick = { onNavigateToShop(link) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = name,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Cached Products list (Offline Cache Resilience)
        item {
            Text(
                text = "کالکشن محبوب ناب لوستر (امکان سفارش آفلاین)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        if (cachedChandeliers.isEmpty() && refreshState is UiState.Loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        } else {
            items(cachedChandeliers) { chandelier ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("chandelier_item_${chandelier.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Product details text inside card
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = chandelier.category,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = chandelier.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = chandelier.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chandelier.price,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                Button(
                                    onClick = { 
                                        onNavigateToShop("https://nabloostr.com/product/${chandelier.id}")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("سفارش آنلاین", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NabloosBrowserScreen(
    url: String,
    onUrlChanged: (String) -> Unit,
    reloadTrigger: Int = 0
) {
    NabloosWebView(
        url = url,
        modifier = Modifier.fillMaxSize(),
        reloadTrigger = reloadTrigger,
        onBackAvailable = { }
    )
}

@Composable
fun ShopScreen(
    url: String,
    onUrlChanged: (String) -> Unit
) {
    var webBackAvailable by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // WebView control panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = { onUrlChanged("https://nabloostr.com/shop") },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = "فروشگاه", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { onUrlChanged("https://nabloostr.com/cart") },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Icon(Icons.Default.ShoppingBag, contentDescription = "سبد خرید", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                text = "مرور امن nabloostr.com",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        NabloosWebView(
            url = url,
            modifier = Modifier.weight(1f),
            onBackAvailable = { webBackAvailable = it }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomDesignScreen(viewModel: NabloosViewModel) {
    val context = LocalContext.current
    val designRequests by viewModel.customDesignRequests.collectAsStateWithLifecycle()
    val submissionState by viewModel.submissionState.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dims by remember { mutableStateOf("") }
    var crystalType by remember { mutableStateOf("کریستال تراش دار عسلی (شامپاینی)") }
    var description by remember { mutableStateOf("") }

    // Alert Handling
    LaunchedEffect(submissionState) {
        if (submissionState is UiState.Success) {
            Toast.makeText(context, (submissionState as UiState.Success<String>).data, Toast.LENGTH_LONG).show()
            name = ""
            phone = ""
            dims = ""
            description = ""
            viewModel.resetSubmissionState()
        } else if (submissionState is UiState.Error) {
            Toast.makeText(context, (submissionState as UiState.Error).message, Toast.LENGTH_LONG).show()
            viewModel.resetSubmissionState()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Image(
                painter = painterResource(id = R.drawable.img_custom_design_illustration),
                contentDescription = "بخش طراحی سفارشی",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "فرم درخواست طراحی اختصاصی لوستر",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "ابعاد و طرح ذهنی خود را ارسال کنید تا در اسرع وقت طراحان کارگاه ناب لوستر نقشه 3D آن را برای شما مدل‌سازی و تولید کنند.",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 12.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام و نام خانوادگی") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("design_input_name"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("شماره تماس همراه") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("design_input_phone"),
                    singleLine = true
                )
                OutlinedTextField(
                    value = dims,
                    onValueChange = { dims = it },
                    label = { Text("ابعاد تقریبی (مثلا: قطر ۸۰، ارتفاع ۱۲۰)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Crystall type row
                Text("نوع کریستال بندی لوستر:", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                val crystalOptions = listOf(
                    "کریستال عسلی شامپاینی",
                    "کریستال شفاف اتریشی",
                    "سواروسکی طلایی",
                    "اس ام دی خطی مدرن"
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    crystalOptions.forEach { opt ->
                        FilterChip(
                            selected = crystalType == opt,
                            onClick = { crystalType = opt },
                            label = { Text(opt, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("توضیحات و ترجیحات (تعداد شاخه، آبکاری طلا/کروم)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 4
                )
                
                Button(
                    onClick = {
                        viewModel.submitCustomDesign(
                            name = name,
                            phone = phone,
                            dimensions = dims,
                            crystalType = crystalType,
                            comment = description
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("design_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ثبت درخواست و هماهنگی", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Submissions checklist Log list
        if (designRequests.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "طرح‌های ثبت شده اخیر شما",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(designRequests) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "طرح کد ${req.id} - ${req.name}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (req.isSubmitted) Color(0xFF2E7D32) else Color(0xFFE65100),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (req.isSubmitted) "ارسال شده" else "آفلاین / در صف",
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "کریستال: ${req.crystalType} | ابعاد: ${req.dimensions}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                        IconButton(onClick = { viewModel.deleteRequest(req.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "حذف", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JournalScreen(viewModel: NabloosViewModel) {
    val articles by viewModel.cachedArticles.collectAsStateWithLifecycle()
    var selectedArticle by remember { mutableStateOf<ArticleEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "مجله تخصصی نورپردازی ناب لوستر",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        items(articles) { art ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedArticle = art },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("مجله ناب لوستر", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = art.date, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = "زمان مطالعه: ${art.readingTime}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = art.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = art.excerpt,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Modal dialog to read full article content beautifully
    selectedArticle?.let { article ->
        Dialog(onDismissRequest = { selectedArticle = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "مقاله آموزشی ناب لوستر",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        IconButton(onClick = { selectedArticle = null }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن")
                        }
                    }
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = article.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "منتشر شده: ${article.date}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                            Text(text = article.readingTime, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = article.content,
                            fontSize = 13.sp,
                            lineHeight = 24.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    var orderCode by remember { mutableStateOf("") }
    var trackingResult by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Customer profile overview
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), Color.Transparent)
                        ),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("کاربر طلایی ناب لوستر", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("عضو باشگاه مشتریان ممتاز", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Live Order Tracing Simulator Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "سامانه پیگیری ساخت و تحویل سفارشات",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "کد سفارش دریافت شده از پیامک یا کارگاه (مانند NBL-1024) را وارد کنید:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = orderCode,
                            onValueChange = { orderCode = it },
                            placeholder = { Text("مثال: NBL-1024") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                trackingResult = when {
                                    orderCode.isBlank() -> "لطفاً کد سفارش خود را وارد کنید."
                                    orderCode.contains("1024", ignoreCase = true) -> 
                                        "📌 وضعیت سفارش: در حال ساخت لوستر فیزیکی\n\nتوضیحات: مونتاژ بدنه اصلی لوستر کریستالی ۱۲ شاخه به اتمام رسیده و سیم‌کشی ایمن با کیفیت غلاف نسوز انجام شد. هم‌اکنون کریستال‌های سفید عسلی ساخت اتریش در بخش سنجاق به آویزها نصب می‌شوند.\n\nزمان تقریبی ارسال کالا: ۳ روز کاری آینده"
                                    orderCode.contains("1025", ignoreCase = true) -> 
                                        "📌 وضعیت سفارش: مونتاژ تمام شده و ارسال به باربری\n\nتوضیحات: فرآیند ساخت لوستر مدرن سیلیکونی ۸۰ سانتی به شکل ۱۰۰٪ کیفیت فونداسیون بررسی و آبکاری تایید شد. کالا بسته‌بندی حباب‌دار شده و تحویل ناوگان حمل‌ونقل ناب لوستر گردید.\n\nتحویل درب منزل هماهنگ می‌شود."
                                    else -> "⚠️ کد سفارش یافت نشد.\n\nدر صورتی که سفارش شما تازه ثبت شده است، ممکن است ۲۴ ساعت زمان ببرد تا شناسه پیگیری ساخت در سیستم کارگاهی ثبت شود."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("پیگیری", fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    trackingResult?.let { result ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = result,
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Quick contacts and phone support lines
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ارتباط مستقیم با کارشناسان ناب لوستر",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "جهت کسب اطلاعات در مورد روند سفارش‌های سفارشی یا تغییرات نقشه آویز خود در ۲۴ ساعت شبانه‌روز می‌توانید تماس بگیرید.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:09112520113"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تماس تلفنی با کارگاه ناب لوستر", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
