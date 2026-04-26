package com.example.c001apk.ui.login

import android.os.Bundle
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import com.example.c001apk.R
import com.example.c001apk.databinding.ActivityLoginBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.ui.main.MainActivity
import com.example.c001apk.util.PrefManager
import com.example.c001apk.util.ToastUtil
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initWebView()
    }

    private fun initWebView() {
        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.userAgentString = PrefManager.userAgent // 使用已有的 UA
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == Constants.URL_COOLAPK) {
                        // 登录成功，获取 Cookie
                        val cookies = CookieManager.getInstance().getCookie(Constants.URL_COOLAPK)
                        val uid = extractCookie(cookies, "uid")
                        val username = extractCookie(cookies, "username")
                        val token = extractCookie(cookies, "token")
                        if (!uid.isNullOrEmpty() && !username.isNullOrEmpty() && !token.isNullOrEmpty()) {
                            PrefManager.setUid(uid)
                            PrefManager.setUsername(username)
                            PrefManager.setToken(token)
                            PrefManager.setIsLogin(true)
                            ToastUtil.showToast("登录成功")
                            finish()
                            startActivity(MainActivity.newIntent(this@LoginActivity))
                        } else {
                            ToastUtil.showToast("登录失败，未获取到凭证")
                        }
                        return true
                    }
                    return false
                }
            }
            loadUrl(Constants.URL_LOGIN)
        }
    }

    private fun extractCookie(cookies: String?, key: String): String? {
        cookies?.split(";")?.forEach {
            val pair = it.trim().split("=")
            if (pair.size == 2 && pair[0] == key) return pair[1]
        }
        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}