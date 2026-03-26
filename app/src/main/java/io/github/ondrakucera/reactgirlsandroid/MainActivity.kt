package io.github.ondrakucera.reactgirlsandroid

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import io.github.ondrakucera.reactgirlsandroid.databinding.ActivityMainBinding

private const val HOME_URL = "https://reactgirls.com/"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Ensure our content is laid out below the system bars (status/navigation bars)
        // so the top of the loaded website is not hidden behind the status bar.
        // We preserve any existing padding that might be defined in XML.
        val initialPaddingLeft = binding.root.paddingLeft
        val initialPaddingTop = binding.root.paddingTop
        val initialPaddingRight = binding.root.paddingRight
        val initialPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                initialPaddingLeft + systemBars.left,
                initialPaddingTop + systemBars.top,
                initialPaddingRight + systemBars.right,
                initialPaddingBottom + systemBars.bottom
            )
            insets
        }

        configureWebView()
        configureSwipeRefresh()
        configureBackNavigation()
        binding.retryButton.setOnClickListener {
            hideError()
            binding.webView.reload()
        }

        if (savedInstanceState == null) {
            binding.webView.loadUrl(HOME_URL)
        }
    }

    private fun configureSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(R.color.purple_500)
        binding.swipeRefresh.setOnChildScrollUpCallback { _, _ ->
            binding.webView.scrollY > 0
        }
        binding.swipeRefresh.setOnRefreshListener {
            binding.webView.reload()
        }
    }

    private fun configureBackNavigation() {
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (binding.webView.canGoBack()) {
                        binding.webView.goBack()
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun configureWebView() {
        binding.webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = false
        }
        binding.webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                return handleUrl(request.url)
            }

            @Deprecated("Deprecated in Java")
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrl(Uri.parse(url))
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                hideError()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.swipeRefresh.isRefreshing = false
                binding.topProgress.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    binding.swipeRefresh.isRefreshing = false
                    binding.topProgress.visibility = View.GONE
                    showError()
                }
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: WebResourceRequest,
                errorResponse: WebResourceResponse
            ) {
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    binding.swipeRefresh.isRefreshing = false
                    binding.topProgress.visibility = View.GONE
                    showError()
                }
            }
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                if (newProgress < 100) {
                    binding.topProgress.visibility = View.VISIBLE
                    binding.topProgress.progress = newProgress
                } else {
                    binding.topProgress.visibility = View.GONE
                }
            }
        }
    }

    private fun handleUrl(uri: Uri): Boolean {
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme == "http" || scheme == "https") {
            val host = uri.host ?: return false
            return if (isReactGirlsHost(host)) {
                false
            } else {
                tryOpenExternal(uri)
            }
        }
        return tryOpenExternal(uri)
    }

    private fun tryOpenExternal(uri: Uri): Boolean {
        return try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
            true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_app_for_link, Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showError() {
        binding.errorContainer.visibility = View.VISIBLE
    }

    private fun hideError() {
        binding.errorContainer.visibility = View.GONE
    }

    override fun onDestroy() {
        binding.webView.apply {
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }

    private fun isReactGirlsHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "reactgirls.com" || h.endsWith(".reactgirls.com")
    }
}
