package com.example.c001apk.ui.login

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.example.c001apk.R
import com.example.c001apk.constant.Constants
import com.example.c001apk.databinding.ActivityLoginBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.util.PrefManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.webView.apply {
            settings.javaScriptEnabled = true
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.userAgentString = Constants.USER_AGENT
            if (android.os.Build.VERSION.SDK_INT >= 21) {
    CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
}
            webViewClient = object : WebViewClient() {
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        if (url == Constants.URL_COOLAPK) {
            val cookie = CookieManager.getInstance().getCookie(Constants.URL_COOLAPK)
            if (!cookie.isNullOrEmpty()) {
                val uid = Regex("uid=([^;]+)").find(cookie)?.groupValues?.get(1)
                val username = Regex("username=([^;]+)").find(cookie)?.groupValues?.get(1)
                val token = Regex("token=([^;]+)").find(cookie)?.groupValues?.get(1)
                if (!uid.isNullOrEmpty() && !username.isNullOrEmpty() && !token.isNullOrEmpty()) {
                    PrefManager.uid = uid
                    PrefManager.username = username
                    PrefManager.token = token
                    PrefManager.isLogin = true
                    Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}
            loadUrl(Constants.URL_LOGIN)
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}