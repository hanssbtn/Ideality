/*package com.example.ideality.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ideality.databinding.ActivityModelViewerBinding

class ModelViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityModelViewerBinding
    private var isLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityModelViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val modelUrl = intent.getStringExtra("modelUrl") ?: run {
            showToast("No model URL provided")
            return finish()
        }

        setupWebView()
        setupBackButton()
        loadModel(modelUrl)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(false)
                builtInZoomControls = false
            }

            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    hideLoading()
                }
            }
        }
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener { finish() }
    }

    private fun loadModel(modelUrl: String) {
        val html = """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <script type="module" src="https://unpkg.com/@google/model-viewer/dist/model-viewer.min.js"></script>
                <style>
                    body { margin: 0; padding: 0; }
                    model-viewer {
                        width: 100%;
                        height: 100vh;
                        background-color: #f5f5f5;
                    }
                </style>
            </head>
            <body>
                <model-viewer
                    src="$modelUrl"
                    camera-controls
                    auto-rotate
                    ar
                    ar-modes="webxr scene-viewer quick-look"
                    environment-image="neutral"
                    shadow-intensity="1"
                    exposure="1"
                    camera-target="0m 0m 0m"
                    camera-orbit="-20deg 75deg 2m"
                ></model-viewer>
            </body>
            </html>
        """.trimIndent()

        binding.webView.loadData(html, "text/html", "UTF-8")
    }

    private fun showLoading() {
        binding.loadingIndicator.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingIndicator.visibility = View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        binding.webView.clearCache(true)
        binding.webView.clearHistory()
        super.onDestroy()
    }
}*/