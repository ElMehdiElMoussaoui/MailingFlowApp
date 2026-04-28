package com.example.appmailing.TheContact


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.example.appmailing.R
import com.example.appmailing.statistique.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddContactActivity : AppCompatActivity() {

    companion object {
        // Extras entrants (édition)
        const val EXTRA_CONTACT_ID    = "extra_contact_id"
        const val EXTRA_CONTACT_NAME  = "extra_contact_name"
        const val EXTRA_CONTACT_EMAIL = "extra_contact_email"
        const val EXTRA_CONTACT_PHONE = "extra_contact_phone"

        // Extras sortants (résultat)
        const val EXTRA_RESULT_ID    = "result_id"
        const val EXTRA_RESULT_NAME  = "result_name"
        const val EXTRA_RESULT_EMAIL = "result_email"
        const val EXTRA_RESULT_PHONE = "result_phone"
    }

    private var editingId: Int = -1

    private lateinit var layoutFullName: TextInputLayout
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var etFullName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnSaveContact: MaterialButton
    private lateinit var tvCancel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_contact)
        bindViews()
        setupToolbar()
        setupSaveButton()
        tvCancel.setOnClickListener { finish() }
        prefillIfEditing()
    }

    private fun bindViews() {
        layoutFullName = findViewById(R.id.layoutFullName)
        layoutEmail    = findViewById(R.id.layoutEmail)
        etFullName     = findViewById(R.id.etFullName)
        etEmail        = findViewById(R.id.etEmail)
        etPhone        = findViewById(R.id.etPhone)
        btnSaveContact = findViewById(R.id.btnSaveContact)
        tvCancel       = findViewById(R.id.tvCancel)
    }

    private fun setupToolbar() {
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.btnSaveIcon).setOnClickListener { saveContact() }
    }

    private fun setupSaveButton() {
        btnSaveContact.setOnClickListener { saveContact() }
    }

    private fun prefillIfEditing() {
        editingId = intent.getIntExtra(EXTRA_CONTACT_ID, -1)
        if (editingId != -1) {
            etFullName.setText(intent.getStringExtra(EXTRA_CONTACT_NAME))
            etEmail.setText(intent.getStringExtra(EXTRA_CONTACT_EMAIL))
            etPhone.setText(intent.getStringExtra(EXTRA_CONTACT_PHONE))
        }
    }

    private fun saveContact() {
        val name  = etFullName.text?.toString()?.trim() ?: ""
        val email = etEmail.text?.toString()?.trim() ?: ""
        val phone = etPhone.text?.toString()?.trim() ?: ""

        if (!validateFields(name, email)) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(applicationContext)

                val contactToSave: Contact
                if (editingId != -1) {
                    contactToSave = Contact(
                        id = editingId,
                        fullName = name,
                        email = email,
                        phone = phone
                    )
                    try {
                        db.contactDao().updateContact(contactToSave)
                        withContext(Dispatchers.Main) {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    } catch (e: android.database.sqlite.SQLiteConstraintException) {
                        withContext(Dispatchers.Main) {
                            layoutEmail.error = "This email already exists for another contact"
                            Toast.makeText(this@AddContactActivity, "Update failed: Email already exists", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    contactToSave = Contact(
                        id = 0,
                        fullName = name,
                        email = email,
                        phone = phone
                    )
                    val resultId = db.contactDao().insertContact(contactToSave)
                    
                    withContext(Dispatchers.Main) {
                        if (resultId == -1L) {
                            layoutEmail.error = "A contact with this email already exists"
                            Toast.makeText(this@AddContactActivity, "Error: Email already exists", Toast.LENGTH_SHORT).show()
                        } else {
                            setResult(Activity.RESULT_OK)
                            finish()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun validateFields(name: String, email: String): Boolean {
        var valid = true
        if (name.isEmpty()) {
            layoutFullName.error = getString(R.string.error_name_required)
            valid = false
        } else {
            layoutFullName.error = null
        }
        if (email.isEmpty()) {
            layoutEmail.error = getString(R.string.error_email_required)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            layoutEmail.error = getString(R.string.error_email_invalid)
            valid = false
        } else {
            layoutEmail.error = null
        }
        return valid
    }
}
