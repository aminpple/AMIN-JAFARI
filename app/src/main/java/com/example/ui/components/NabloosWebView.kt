package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NabloosWebView(
    url: String,
    modifier: Modifier = Modifier,
    reloadTrigger: Int = 0,
    onBackAvailable: (Boolean) -> Unit = {}
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBackState by remember { mutableStateOf(false) }

    var lastLoadedUrl by remember { mutableStateOf("") }

    // Intercept hardware back clicks to control WebView page back navigation reactively
    BackHandler(enabled = canGoBackState) {
        webViewInstance?.goBack()
    }

    // Refresh page when reload trigger changes
    LaunchedEffect(reloadTrigger) {
        if (reloadTrigger > 0) {
            webViewInstance?.reload()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    
                    // Native fast scroll performance settings and nested scroll support
                    isVerticalScrollBarEnabled = true
                    isHorizontalScrollBarEnabled = false
                    isScrollbarFadingEnabled = true
                    scrollBarStyle = WebView.SCROLLBARS_INSIDE_OVERLAY
                    isNestedScrollingEnabled = true
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        @Suppress("DEPRECATION")
                        databaseEnabled = true
                        
                        // Disable scale computations and gesture delays for modern mobile-responsive sites
                        loadWithOverviewMode = false
                        useWideViewPort = false
                        setSupportZoom(false)
                        builtInZoomControls = false
                        displayZoomControls = false
                        
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        cacheMode = WebSettings.LOAD_DEFAULT
                        
                        // Performance and Speed Enhancements
                        offscreenPreRaster = true
                        loadsImagesAutomatically = true
                        blockNetworkImage = false
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            val canBack = view?.canGoBack() == true
                            canGoBackState = canBack
                            onBackAvailable(canBack)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            val canBack = view?.canGoBack() == true
                            canGoBackState = canBack
                            onBackAvailable(canBack)
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            progress = newProgress / 100f
                            isLoading = newProgress < 100
                            val canBack = view?.canGoBack() == true
                            canGoBackState = canBack
                            onBackAvailable(canBack)
                        }
                    }

                    // Setup Web Cookie permissions
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    loadUrl(url)
                    lastLoadedUrl = url
                    webViewInstance = this
                }
            },
            update = { webView ->
                val normalizedPassed = url.trim().trimEnd('/')
                val normalizedLast = lastLoadedUrl.trim().trimEnd('/')
                if (normalizedPassed.isNotEmpty() && normalizedPassed != normalizedLast) {
                    lastLoadedUrl = url
                    webView.loadUrl(url)
                }
            }
        )

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
        }
    }
}
