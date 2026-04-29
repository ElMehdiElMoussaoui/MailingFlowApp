package com.example.appmailing.TheContact

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.example.appmailing.R
import com.example.appmailing.statistique.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTACT_ID         = "detail_contact_id"
        const val EXTRA_CONTACT_NAME       = "detail_contact_name"
        const val EXTRA_CONTACT_EMAIL      = "detail_contact_email"
        const val EXTRA_CONTACT_PHONE      = "detail_contact_phone"
        const val EXTRA_EMAIL_STATUS       = "detail_email_status"
        const val EXTRA_SENDING_STATUS     = "detail_sending_status"
    }

    private var contactId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_detail)

        contactId = intent.getIntExtra(EXTRA_CONTACT_ID, -1)
        val db = AppDatabase.getDatabase(applicationContext)

        if (contactId != -1) {
            db.contactDao().getContactById(contactId).observe(this) { updatedContact ->
                updatedContact?.let {
                    setupUI(it.fullName, it.email, it.phone, it.emailStatus, it.sendingStatus)
                }
            }
        }

        findViewById<ImageButton>(R.id.btnDetailBack).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnDetailEdit).setOnClickListener {
            val name  = findViewById<TextView>(R.id.tvDetailName).text.toString()
            val email = findViewById<TextView>(R.id.tvDetailEmail).text.toString()
            val phone = findViewById<TextView>(R.id.tvDetailPhone).text.toString()

            val editIntent = Intent(this, AddContactActivity::class.java).apply {
                putExtra(AddContactActivity.EXTRA_CONTACT_ID,    contactId)
                putExtra(AddContactActivity.EXTRA_CONTACT_NAME,  name)
                putExtra(AddContactActivity.EXTRA_CONTACT_EMAIL, email)
                putExtra(AddContactActivity.EXTRA_CONTACT_PHONE, phone)
            }
            startActivity(editIntent)
        }

        findViewById<MaterialButton>(R.id.btnDetailDelete).setOnClickListener {
            val contactName = findViewById<TextView>(R.id.tvDetailName).text.toString()
            
            AlertDialog.Builder(this)
                .setTitle("Delete Contact")
                .setMessage("Are you sure you want to delete $contactName?")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val contactToDelete = Contact(
                            id = contactId,
                            fullName = contactName,
                            email = findViewById<TextView>(R.id.tvDetailEmail).text.toString()
                        )
                        db.contactDao().deleteContact(contactToDelete)
                        withContext(Dispatchers.Main) { finish() }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun setupUI(name: String, email: String, phone: String, emailStatus: EmailStatus, sendingStatus: SendingStatus) {
        val tvInitials = findViewById<TextView>(R.id.tvDetailInitials)
        val parts = name.trim().split(" ")
        tvInitials.text = when {
            parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
            parts.isNotEmpty() -> parts[0].take(2).uppercase()
            else -> "?"
        }

        findViewById<TextView>(R.id.tvDetailName).text  = name
        findViewById<TextView>(R.id.tvDetailEmail).text = email
        findViewById<TextView>(R.id.tvDetailPhone).text = phone.ifBlank { "Non renseigné" }

        val ivEmail = findViewById<ImageView>(R.id.ivDetailEmailStatus)
        val tvEmailStatus = findViewById<TextView>(R.id.tvDetailEmailStatus)
        if (emailStatus == EmailStatus.CONFORME) {
            ivEmail.setImageResource(R.drawable.ic_check_circle)
            ivEmail.setColorFilter(getColor(R.color.green_500))
            tvEmailStatus.text = "Conforme"
            tvEmailStatus.setTextColor(getColor(R.color.green_500))
        } else {
            ivEmail.setImageResource(R.drawable.ic_error_circle)
            ivEmail.setColorFilter(getColor(R.color.red_400))
            tvEmailStatus.text = "Non Conforme"
            tvEmailStatus.setTextColor(getColor(R.color.red_400))
        }

        val ivSending = findViewById<ImageView>(R.id.ivDetailSendingStatus)
        val tvSendingStatus = findViewById<TextView>(R.id.tvDetailSendingStatus)
        
        when (sendingStatus) {
            SendingStatus.SENT -> {
                ivSending.setImageResource(R.drawable.ic_email_sent)
                ivSending.setColorFilter(getColor(R.color.blue_primary))
                tvSendingStatus.text = "Envoyé"
                tvSendingStatus.setTextColor(getColor(R.color.blue_primary))
            }
            SendingStatus.FAILED -> {
                ivSending.setImageResource(R.drawable.ic_error_circle)
                ivSending.setColorFilter(getColor(R.color.red_400))
                tvSendingStatus.text = "Échoué"
                tvSendingStatus.setTextColor(getColor(R.color.red_400))
            }
            else -> {
                ivSending.setImageResource(R.drawable.ic_email_unsent)
                ivSending.setColorFilter(getColor(R.color.grey_400))
                tvSendingStatus.text = "Non Envoyé"
                tvSendingStatus.setTextColor(getColor(R.color.grey_400))
            }
        }
    }
}
