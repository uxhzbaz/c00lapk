package com.example.c001apk.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.c001apk.R
import com.example.c001apk.constant.Constants
import com.example.c001apk.databinding.ActivityLoginBinding
import com.example.c001apk.ui.base.BaseActivity
import com.example.c001apk.ui.others.WebViewActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
            finish()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(binding.toolBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        launcher.launch(Intent(this, WebViewActivity::class.java).apply {
            putExtra("url", Constants.URL_LOGIN)
            putExtra("isLogin", true)
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isFinishing) {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return true
    }
}