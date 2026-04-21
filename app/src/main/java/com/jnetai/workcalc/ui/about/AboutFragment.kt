package com.jnetai.workcalc.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.jnetai.workcalc.BuildConfig
import com.jnetai.workcalc.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class AboutFragment : Fragment() {

    private lateinit var textVersion: TextView
    private lateinit var textUpdateStatus: TextView
    private lateinit var btnCheckUpdates: MaterialButton
    private lateinit var btnGithub: MaterialButton
    private lateinit var btnShareApp: MaterialButton

    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_about, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        textVersion = view.findViewById(R.id.text_version)
        textUpdateStatus = view.findViewById(R.id.text_update_status)
        btnCheckUpdates = view.findViewById(R.id.btn_check_updates)
        btnGithub = view.findViewById(R.id.btn_github)
        btnShareApp = view.findViewById(R.id.btn_share_app)

        textVersion.text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

        btnCheckUpdates.setOnClickListener { checkForUpdates() }

        btnGithub.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/jnetai-clawbot/Work-Calc-Pro"))
            startActivity(intent)
        }

        btnShareApp.setOnClickListener {
            val shareText = """
                Work Calc Pro - UK Wage Calculator
                Track shifts, calculate pay, tax, NI & UC deductions.
                Download: https://github.com/jnetai-clawbot/Work-Calc-Pro/releases
            """.trimIndent()
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(intent, "Share App"))
        }
    }

    private fun checkForUpdates() {
        textUpdateStatus.visibility = View.VISIBLE
        textUpdateStatus.text = "Checking..."
        btnCheckUpdates.isEnabled = false

        lifecycleScope.launch {
            try {
                val latest = withContext(Dispatchers.IO) { fetchLatestRelease() }
                if (latest != null) {
                    val tagName = latest.optString("tag_name", "")
                    val htmlUrl = latest.optString("html_url", "")
                    if (tagName.isNotEmpty() && tagName != "v${BuildConfig.VERSION_NAME}") {
                        textUpdateStatus.text = getString(R.string.update_available, tagName)
                        textUpdateStatus.setTextColor(
                            resources.getColor(R.color.md_theme_dark_primary, null)
                        )
                        // Open release page
                        if (htmlUrl.isNotEmpty()) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(htmlUrl)))
                        }
                    } else {
                        textUpdateStatus.text = getString(R.string.up_to_date)
                        textUpdateStatus.setTextColor(
                            resources.getColor(R.color.shift_dot_green, null)
                        )
                    }
                } else {
                    textUpdateStatus.text = getString(R.string.update_check_failed)
                    textUpdateStatus.setTextColor(
                        resources.getColor(R.color.md_theme_dark_error, null)
                    )
                }
            } catch (e: Exception) {
                textUpdateStatus.text = getString(R.string.update_check_failed)
                textUpdateStatus.setTextColor(
                    resources.getColor(R.color.md_theme_dark_error, null)
                )
            }
            btnCheckUpdates.isEnabled = true
        }
    }

    private fun fetchLatestRelease(): JSONObject? {
        return try {
            val request = Request.Builder()
                .url("https://api.github.com/repos/jnetai-clawbot/Work-Calc-Pro/releases/latest")
                .build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                JSONObject(response.body?.string() ?: "")
            } else null
        } catch (e: Exception) {
            null
        }
    }
}