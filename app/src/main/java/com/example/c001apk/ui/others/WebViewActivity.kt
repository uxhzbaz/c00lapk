package com.example.c001apk.ui.others

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.example.c001apk.R
import com.example.c001apk.constant.Constants
import com.example.c001apk.databinding.ActivityWebViewBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.ClipboardUtil.copyText
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.http2https
import com.google.android.material.appbar.AppBarLayout.ScrollingViewBehavior
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.net.URISyntaxException
import java.net.URLDecoder

class WebViewActivity : BaseActivity<ActivityWebViewBinding>() {

    private val url by lazy { intent.getStringExtra("url") }
    private val isLogin by lazy { intent.getBooleanExtra("isLogin", false) }
    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        runCatching {
            webView = WebView(this).apply {
                layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
                ).apply {
                    behavior = ScrollingViewBehavior()
                }
            }
            binding.root.addView(webView)
        }.onFailure {
            MaterialAlertDialogBuilder(this)
                .setTitle("Failed to init WebView")
                .setMessage(it.message)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton("Log") { _, _ ->
                    MaterialAlertDialogBuilder(this)
                        .setTitle("Log")
                        .setMessage(it.stackTraceToString())
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
                .show()
            return
        }

        url?.let { loadUrlInWebView(it) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadUrlInWebView(url: String) {
        webView?.let {
            it.settings.apply {
                javaScriptEnabled = true
                blockNetworkImage = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                domStorageEnabled = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                defaultTextEncodingName = "UTF-8"
                allowContentAccess = true
                useWideViewPort = true
                loadWithOverviewMode = true
                javaScriptCanOpenWindowsAutomatically = true
                loadsImagesAutomatically = true
                allowFileAccess = false
                userAgentString = PrefManager.USER_AGENT
                if (SDK_INT >= 32) {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(this, true)
                    }
                } else {
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                        val nightModeFlags =
                            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                            WebSettingsCompat.setForceDark(this, WebSettingsCompat.FORCE_DARK_ON)
                        }
                    }
                }
            }

            val cookieManager = CookieManager.getInstance().apply {
                setAcceptThirdPartyCookies(webView, true)   // 登录需要接收第三方 Cookie
                if (isLogin) removeAllCookies(null)
                if (PrefManager.isLogin) {
                    setCookie(".coolapk.com", "uid=${PrefManager.uid}")
                    setCookie(".coolapk.com", "username=${PrefManager.username}")
                    setCookie(".coolapk.com", "token=${PrefManager.token}")
                }
                setCookie("m.coolapk.com", "DID=${PrefManager.SZLMID}")
                setCookie("m.coolapk.com", "forward=https://www.coolapk.com")
                setCookie("m.coolapk.com", "displayVersion=v14")
            }

            it.apply {
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, _ ->
                    val fileName = URLDecoder.decode(
                        URLUtil.guessFileName(downloadUrl, contentDisposition, mimetype),
                        "UTF-8"
                    )
                    MaterialAlertDialogBuilder(this@WebViewActivity).apply {
                        setTitle("确定下载文件吗？")
                        setMessage(fileName)
                        setNeutralButton("外部打开") { _, _ ->
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl)))
                            } catch (e: ActivityNotFoundException) {
                                Toast.makeText(this@WebViewActivity, "打开失败", Toast.LENGTH_SHORT)
                                    .show()
                                copyText(this@WebViewActivity, downloadUrl)
                            }
                        }
                        setNegativeButton(android.R.string.cancel, null)
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            try {
                                val request = DownloadManager.Request(Uri.parse(downloadUrl))
                                    .setMimeType(mimetype)
                                    .addRequestHeader("cookie", cookieManager.getCookie(downloadUrl) ?: "")
                                    .addRequestHeader("User-Agent", userAgent)
                                    .setTitle(fileName)
                                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                dm.enqueue(request)
                            } catch (e: Exception) {
                                Toast.makeText(this@WebViewActivity, "下载失败", Toast.LENGTH_SHORT)
                                    .show()
                                copyText(this@WebViewActivity, downloadUrl)
                            }
                        }
                        show()
                    }
                }

                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        request?.let {
                            try {
                                val currentUrl = it.url.toString()
                                // 登录成功检测
                                if (isLogin && currentUrl == Constants.URL_COOLAPK) {
                                    val cookie = cookieManager.getCookie(Constants.URL_COOLAPK) ?: ""
                                    val uid = Regex("uid=([^;]+)").find(cookie)?.groupValues?.get(1)
                                    val username = Regex("username=([^;]+)").find(cookie)?.groupValues?.get(1)
                                    val token = Regex("token=([^;]+)").find(cookie)?.groupValues?.get(1)
                                    if (!uid.isNullOrEmpty() && !username.isNullOrEmpty() && !token.isNullOrEmpty()) {
                                        PrefManager.uid = uid
                                        PrefManager.username = username
                                        PrefManager.token = token
                                        PrefManager.isLogin = true
                                        Toast.makeText(this@WebViewActivity, "登录成功", Toast.LENGTH_SHORT).show()
                                        setResult(RESULT_OK)
                                        finish()
                                        return true
                                    } else {
                                        Toast.makeText(this@WebViewActivity, "登录失败，未获取到凭证", Toast.LENGTH_SHORT).show()
                                        finish()
                                        return true
                                    }
                                }
                                // intent 协议
                                if (currentUrl.startsWith("intent://")) {
                                    try {
                                        val intent = Intent.parseUri(currentUrl, Intent.URI_INTENT_SCHEME).apply {
                                            addCategory("android.intent.category.BROWSABLE")
                                            component = null
                                            selector = null
                                        }
                                        if (packageManager.queryIntentActivities(intent, 0).size > 0) {
                                            startActivityIfNeeded(intent, -1)
                                        }
                                        return true
                                    } catch (e: URISyntaxException) {
                                        e.printStackTrace()
                                    }
                                }
                                // 非 http 链接提示外部打开
                                if (!currentUrl.startsWith("http")) {
                                    view?.let {
                                        Snackbar.make(
                                            it,
                                            "当前网页将要打开外部链接，是否打开",
                                            Snackbar.LENGTH_SHORT
                                        ).setAction("打开") {
                                            try {
                                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl)).apply {
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                                })
                                            } catch (e: Exception) {
                                                Toast.makeText(this@WebViewActivity, "打开失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }.show()
                                    }
                                    return true
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        return super.shouldOverrideUrlLoading(view, request)
                    }
                }

                webChromeClient = object : WebChromeClient() {
                    override fun onCloseWindow(window: WebView?) {
                        super.onCloseWindow(window)
                        finish()
                    }
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        if (newProgress == 100) {
                            binding.progressBar.isVisible = false
                        } else {
                            binding.progressBar.isVisible = true
                            binding.progressBar.progress = newProgress
                        }
                    }
                    override fun onReceivedTitle(view: WebView, title: String) {
                        super.onReceivedTitle(view, title)
                        binding.toolBar.title = title
                    }
                }
                loadUrl(url, mapOf("X-Requested-With" to "com.coolapk.market"))
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.webview_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.refresh -> webView?.reload()
            R.id.copyLink -> webView?.url?.let { copyText(this, it.http2https) }
            R.id.openInBrowser -> webView?.url?.let {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.http2https)))
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show()
                    Log.w("error", "Activity was not found for intent")
                }
            }
            R.id.clearCache -> {
                webView?.apply {
                    clearHistory()
                    clearCache(true)
                    clearFormData()
                    Toast.makeText(this@WebViewActivity, "清除缓存成功", Toast.LENGTH_SHORT).show()
                }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView?.canGoBack() == true) {
            webView?.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        try {
            webView?.apply {
                loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
                loadUrl("about:blank")
                parent?.let { (it as ViewGroup).removeView(this) }
                stopLoading()
                settings.javaScriptEnabled = false
                clearHistory()
                clearCache(true)
                removeAllViewsInLayout()
                removeAllViews()
                setOnTouchListener(null)
                setOnKeyListener(null)
                onFocusChangeListener = null
                webChromeClient = null
                onPause()
                destroy()
            }
            webView = null
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        super.onDestroy()
    }
}