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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import java.net.URISyntaxException
import java.net.URLDecoder
import kotlin.system.exitProcess

class WebViewActivity : BaseActivity<ActivityWebViewBinding>() {

    private var link: String? = null
    private var isLogin = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        link = intent.getStringExtra("url")
        isLogin = intent.getBooleanExtra("isLogin", false)
        link?.let { loadUrlInWebView(it.http2https) }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun loadUrlInWebView(url: String) {
        binding.webView.settings.also {
            it.javaScriptEnabled = true
            it.domStorageEnabled = true
            it.setSupportZoom(true)
            it.builtInZoomControls = true
            it.displayZoomControls = false
            it.cacheMode = WebSettings.LOAD_NO_CACHE
            it.defaultTextEncodingName = "UTF-8"
            it.allowContentAccess = true
            it.useWideViewPort = true
            it.loadWithOverviewMode = true
            it.javaScriptCanOpenWindowsAutomatically = true
            it.loadsImagesAutomatically = true
            it.allowFileAccess = false
            it.userAgentString = PrefManager.USER_AGENT
            if (SDK_INT >= 32) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING))
                    WebSettingsCompat.setAlgorithmicDarkeningAllowed(it, true)
            } else {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) &&
                    (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                ) WebSettingsCompat.setForceDark(it, WebSettingsCompat.FORCE_DARK_ON)
            }
        }

        val cm = CookieManager.getInstance().apply {
            setAcceptThirdPartyCookies(binding.webView, true)
            if (isLogin) {
                removeAllCookies(null)
            }
            if (PrefManager.isLogin) {
                setCookie("https://www.coolapk.com", "uid=${PrefManager.uid}")
                setCookie("https://www.coolapk.com", "username=${PrefManager.username}")
                setCookie("https://www.coolapk.com", "token=${PrefManager.token}")
                setCookie("m.coolapk.com", "DID=${PrefManager.SZLMID}")
                setCookie("m.coolapk.com", "forward=https://www.coolapk.com")
                setCookie("m.coolapk.com", "displayVersion=v14")
                setCookie("m.coolapk.com", "uid=${PrefManager.uid}")
                setCookie("m.coolapk.com", "username=${PrefManager.username}")
                setCookie("m.coolapk.com", "token=${PrefManager.token}")
            } else {
                setCookie("m.coolapk.com", "DID=${PrefManager.SZLMID}")
                setCookie("m.coolapk.com", "forward=https://www.coolapk.com")
                setCookie("m.coolapk.com", "displayVersion=v14")
            }
        }

        binding.webView.apply {
            setDownloadListener { url, ua, cd, mime, _ ->
                val fn = URLDecoder.decode(URLUtil.guessFileName(url, cd, mime), "UTF-8")
                MaterialAlertDialogBuilder(this@WebViewActivity).apply {
                    setTitle("确定下载文件吗？")
                    setMessage(fn)
                    setNeutralButton("外部打开") { _, _ ->
                        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                        catch (e: ActivityNotFoundException) {
                            Toast.makeText(this@WebViewActivity, "打开失败", Toast.LENGTH_SHORT).show()
                            copyText(this@WebViewActivity, url)
                        }
                    }
                    setNegativeButton(android.R.string.cancel, null)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        try {
                            val req = DownloadManager.Request(Uri.parse(url))
                                .setMimeType(mime)
                                .addRequestHeader("cookie", cm.getCookie(url) ?: "")
                                .addRequestHeader("User-Agent", ua)
                                .setTitle(fn)
                                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fn)
                            (getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(req)
                        } catch (e: Exception) {
                            Toast.makeText(this@WebViewActivity, "下载失败", Toast.LENGTH_SHORT).show()
                            copyText(this@WebViewActivity, url)
                        }
                    }
                    show()
                }
            }
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView?, req: WebResourceRequest?): Boolean {
                    req?.let { r ->
                        try {
                            val u = r.url.toString()
                            if (isLogin && u.startsWith(Constants.URL_COOLAPK)) {
                                val cookie = cm.getCookie(Constants.URL_COOLAPK) ?: ""
                                val uid = Regex("uid=([^;]+)").find(cookie)?.groupValues?.get(1)
                                val uname = Regex("username=([^;]+)").find(cookie)?.groupValues?.get(1)
                                val token = Regex("token=([^;]+)").find(cookie)?.groupValues?.get(1)
                                if (!uid.isNullOrEmpty() && !uname.isNullOrEmpty() && !token.isNullOrEmpty()) {
                                    PrefManager.uid = uid
                                    PrefManager.username = uname
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
                            if (u.startsWith("intent://")) {
                                try {
                                    val it = Intent.parseUri(u, Intent.URI_INTENT_SCHEME).apply {
                                        addCategory("android.intent.category.BROWSABLE")
                                        component = null
                                        selector = null
                                    }
                                    if (packageManager.queryIntentActivities(it, 0).size > 0)
                                        startActivityIfNeeded(it, -1)
                                    return true
                                } catch (e: URISyntaxException) { e.printStackTrace() }
                            }
                            if (!u.startsWith("http")) {
                                v?.let {
                                    Snackbar.make(it, "当前网页将要打开外部链接，是否打开", Snackbar.LENGTH_SHORT)
                                        .setAction("打开") {
                                            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                            }) } catch (e: Exception) {
                                                Toast.makeText(this@WebViewActivity, "打开失败", Toast.LENGTH_SHORT).show()
                                            }
                                        }.show()
                                }
                                return true
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                    return super.shouldOverrideUrlLoading(v, req)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(v: WebView?, p: Int) {
                    if (p == 100) binding.progressBar.isVisible = false
                    else {
                        binding.progressBar.isVisible = true
                        binding.progressBar.progress = p
                    }
                }
                override fun onReceivedTitle(v: WebView, t: String) {
                    super.onReceivedTitle(v, t)
                    binding.toolBar.title = t
                }
            }
            loadUrl(url, mapOf("X-Requested-With" to "com.coolapk.market"))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?) = menuInflater.inflate(R.menu.webview_menu, menu).let { true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> finish().let { true }
        R.id.refresh -> binding.webView.reload().let { true }
        R.id.copyLink -> binding.webView.url?.let { copyText(this, it.http2https) }.let { true }
        R.id.openInBrowser -> binding.webView.url?.let {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it.http2https))) }
            catch (e: ActivityNotFoundException) { Toast.makeText(this, "打开失败", Toast.LENGTH_SHORT).show() }
        }.let { true }
        R.id.clearCache -> {
            binding.webView.run { clearHistory(); clearCache(true); clearFormData() }
            Toast.makeText(this, "清除缓存成功", Toast.LENGTH_SHORT).show(); true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(kc: Int, e: KeyEvent?): Boolean {
        if (kc == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack()) {
            binding.webView.goBack()
            return true
        }
        return super.onKeyDown(kc, e)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onDestroy() {
        try {
            binding.webView.apply {
                loadDataWithBaseURL(null, "", "text/html", "utf-8", null)
                loadUrl("about:blank")
                (parent as? ViewGroup)?.removeView(this)
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
        } catch (_: Throwable) {}
        super.onDestroy()
    }
}