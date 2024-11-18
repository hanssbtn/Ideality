package com.example.ideality.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.example.ideality.R
import com.example.ideality.databinding.ActivityContactUsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class ContactUsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityContactUsBinding
    private val emailPrefs by lazy {
        getSharedPreferences("email_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        private const val MAX_EMAILS_PER_HOUR = 3
        private const val COOLDOWN_PERIOD = 3600000L // 1 hour in milliseconds
        private const val TAG = "ContactUsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStatusBar()
        setupToolbar()
        setupInputValidation()
        setupClickListeners()
        setupAnimations()
    }

    private fun canSendEmail(): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastEmailTime = emailPrefs.getLong("last_email_time", 0)
        val emailCount = emailPrefs.getInt("email_count", 0)
        val cooldownEndTime = emailPrefs.getLong("cooldown_end_time", 0)

        if (currentTime < cooldownEndTime) {
            val remainingMinutes = (cooldownEndTime - currentTime) / 60000
            showError("Please wait $remainingMinutes minutes before sending another message")
            return false
        }

        if (currentTime - lastEmailTime > COOLDOWN_PERIOD) {
            emailPrefs.edit().apply {
                putInt("email_count", 0)
                putLong("last_email_time", currentTime)
                apply()
            }
            return true
        }

        if (emailCount >= MAX_EMAILS_PER_HOUR) {
            val cooldownEnd = currentTime + COOLDOWN_PERIOD
            emailPrefs.edit().apply {
                putLong("cooldown_end_time", cooldownEnd)
                apply()
            }
            showError("Maximum messages limit reached. Please try again later.")
            return false
        }

        return true
    }

    private fun updateEmailStats() {
        emailPrefs.edit().apply {
            putLong("last_email_time", System.currentTimeMillis())
            putInt("email_count", emailPrefs.getInt("email_count", 0) + 1)
            apply()
        }
    }

    private fun setupStatusBar() {
        window.statusBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun setupToolbar() {
        binding.backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupInputValidation() {
        binding.apply {
            subjectInput.addTextChangedListener { validateInputs() }
            messageInput.addTextChangedListener { validateInputs() }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            emailCard.setOnClickListener {
                animateCardPress(emailCard)
                openEmailClient()
            }

            faqCard.setOnClickListener {
                animateCardPress(faqCard)
                navigateToFaq()
            }

            sendButton.setOnClickListener {
                if (validateInputs()) {
                    sendMessage()
                }
            }
        }
    }

    private fun setupAnimations() {
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        val slideDown = AnimationUtils.loadAnimation(this, R.anim.slide_down)

        binding.apply {
            val views = listOf(
                emailCard,
                messageCard,
                faqCard
            )

            views.forEachIndexed { index, view ->
                view.alpha = 0f
                view.postDelayed({
                    view.alpha = 1f
                    view.startAnimation(fadeIn)
                    view.startAnimation(slideDown)
                }, index * 100L)
            }
        }
    }

    private fun animateCardPress(view: View) {
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        binding.apply {
            if (subjectInput.text.isNullOrBlank()) {
                subjectInputLayout.error = "Please enter a subject"
                isValid = false
            } else {
                subjectInputLayout.error = null
            }

            if (messageInput.text.isNullOrBlank()) {
                messageInputLayout.error = "Please enter your message"
                isValid = false
            } else if (messageInput.text.toString().length < 10) {
                messageInputLayout.error = "Message is too short"
                isValid = false
            } else {
                messageInputLayout.error = null
            }

            sendButton.isEnabled = isValid
        }
        return isValid
    }

    private fun sendMessage() {
        if (!canSendEmail()) {
            return
        }

        val subject = binding.subjectInput.text.toString()
        val message = binding.messageInput.text.toString()

        val loadingDialog = showLoadingDialog()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                sendEmailDirectly(subject, message)

                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    updateEmailStats()
                    clearInputs()
                    showSuccessMessage()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loadingDialog.dismiss()
                    showError("Failed to send email: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun showLoadingDialog(): AlertDialog {
        return MaterialAlertDialogBuilder(this, R.style.TransparentDialog)
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .show().apply {
                window?.setBackgroundDrawableResource(android.R.color.transparent)
                // Add animation
                window?.decorView?.startAnimation(
                    AnimationUtils.loadAnimation(context, R.anim.dialog_scale_in)
                )
            }
    }

    private fun clearInputs() {
        binding.apply {
            subjectInput.text?.clear()
            messageInput.text?.clear()
        }
    }

    private suspend fun sendEmailDirectly(subject: String, messageBody: String) {
        withContext(Dispatchers.IO) {
            val props = Properties()
            props.put("mail.smtp.auth", "true")
            props.put("mail.smtp.starttls.enable", "true")
            props.put("mail.smtp.host", "smtp.gmail.com")
            props.put("mail.smtp.port", "587")

            val session = Session.getInstance(props, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        "mhemail12345@gmail.com",
                        "cpol ejud mvxg uony"
                    )
                }
            })

            try {
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress("mhemail12345@gmail.com"))
                    addRecipient(Message.RecipientType.TO, InternetAddress("mhemail12345@gmail.com"))
                    setSubject("Ideality Support: $subject")
                    setText(messageBody)
                }

                Transport.send(message)
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun openEmailClient() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf("mhemail12345@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "Ideality Support")
        }

        try {
            startActivity(Intent.createChooser(intent, "Send email using..."))
        } catch (e: Exception) {
            showError("No email client found on device")
        }
    }

    private fun navigateToFaq() {
        startActivity(Intent(this, FaqActivity::class.java))
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun showSuccessMessage() {
        Snackbar.make(
            binding.root,
            "Message sent successfully",
            Snackbar.LENGTH_LONG
        ).setAction("OK") {}.show()
    }

    private fun showError(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_LONG
        ).setAction("Retry") {
            sendMessage()
        }.show()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}