package com.example.ideality.activities

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import com.example.ideality.R
import com.example.ideality.databinding.ActivityPrivacyPolicyBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject

class PrivacyPolicyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPrivacyPolicyBinding
    private lateinit var remoteConfig: FirebaseRemoteConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrivacyPolicyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFirebaseRemoteConfig()
        setupStatusBar()
        setupToolbar()
        setupScrollBehavior()
        setupAnimations()

        if (savedInstanceState == null) {
            fetchRemoteConfig()
        }
    }

    private fun setupFirebaseRemoteConfig() {
        remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            // Use a fixed value for fetch interval
            .setMinimumFetchIntervalInSeconds(3600) // 1 hour
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    }

    private fun setupStatusBar() {
        window.statusBarColor = Color.parseColor("#FFFFFF")
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupToolbar() {
        supportActionBar?.hide()

        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        var isToolbarElevated = false
        binding.apply {
            nestedScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                if (scrollY > 0 && !isToolbarElevated) {
                    toolbar.elevation = resources.getDimension(R.dimen.toolbar_elevation)
                    divider.elevation = resources.getDimension(R.dimen.toolbar_elevation)
                    isToolbarElevated = true
                } else if (scrollY == 0 && isToolbarElevated) {
                    toolbar.elevation = 0f
                    divider.elevation = 0f
                    isToolbarElevated = false
                }
            }
        }
    }

    private fun setupScrollBehavior() {
        binding.apply {
            nestedScrollView.isSmoothScrollingEnabled = true
            nestedScrollView.isNestedScrollingEnabled = true
        }
    }

    private fun setupAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)
        val cardElevation = AnimationUtils.loadAnimation(this, R.anim.card_elevation)
        val cardElevationReset = AnimationUtils.loadAnimation(this, R.anim.card_elevation_reset)

        binding.apply {
            // Apply animations to sections with delay
            val sections = arrayOf(introductionSection, informationSection, usageSection, securitySection)
            sections.forEachIndexed { index, view ->
                view.alpha = 0f
                view.postDelayed({
                    view.alpha = 1f
                    view.startAnimation(fadeIn)
                    view.startAnimation(slideDown)
                }, index * 100L)
            }

            // Add elevation and touch feedback animations to cards
            arrayOf(introductionCard, informationCard, usageCard, securityCard).forEach { card ->
                card.setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            view.startAnimation(cardElevation)
                            view.animate().scaleX(0.98f).scaleY(0.98f).setDuration(200).start()
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            view.startAnimation(cardElevationReset)
                            view.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    private fun fetchRemoteConfig() {
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    try {
                        val policyJson = remoteConfig.getString("privacy_policy")
                        val policyObject = JSONObject(policyJson)

                        updateContent(policyObject)
                    } catch (e: Exception) {
                        showError("Failed to parse privacy policy content")
                    }
                } else {
                    showError("Failed to fetch privacy policy")
                }
            }
    }

    private fun updateContent(policyObject: JSONObject) {
        binding.loadingProgressBar.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                binding.loadingProgressBar.visibility = View.GONE
            }

        binding.contentLayout.visibility = View.VISIBLE
        binding.contentLayout.alpha = 0f
        binding.contentLayout.animate()
            .alpha(1f)
            .setDuration(300)

        binding.apply {
            introductionText.text = policyObject.getString("introduction")

            // Update information list with animation
            informationList.removeAllViews()
            val infoArray = policyObject.getJSONArray("informationWeCollect")
            for (i in 0 until infoArray.length()) {
                val infoView = layoutInflater.inflate(R.layout.item_bullet_point, informationList, false)
                infoView.findViewById<TextView>(R.id.bulletText).text = infoArray.getString(i)
                infoView.alpha = 0f
                informationList.addView(infoView)
                infoView.animate()
                    .alpha(1f)
                    .setStartDelay(i * 100L)
                    .setDuration(300)
                    .start()
            }

            // Update usage list with animation
            usageList.removeAllViews()
            val usageArray = policyObject.getJSONArray("howWeUseInfo")
            for (i in 0 until usageArray.length()) {
                val usageView = layoutInflater.inflate(R.layout.item_bullet_point, usageList, false)
                usageView.findViewById<TextView>(R.id.bulletText).text = usageArray.getString(i)
                usageView.alpha = 0f
                usageList.addView(usageView)
                usageView.animate()
                    .alpha(1f)  // Add this line
                    .setStartDelay(i * 100L)
                    .setDuration(300)
                    .start()
            }

            securityText.text = policyObject.getString("dataSecurity")
        }
    }

    private fun showError(error: String) {
        binding.loadingProgressBar.visibility = View.GONE
        Snackbar.make(binding.root, error, Snackbar.LENGTH_INDEFINITE)
            .setAction("Retry") {
                fetchRemoteConfig()
            }
            .show()
    }

    companion object {
        private const val TAG = "PrivacyPolicyActivity"
    }
}