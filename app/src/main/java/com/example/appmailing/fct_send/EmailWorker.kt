package com.example.appmailing.fct_send

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.appmailing.statistique.AppDatabase
import com.example.appmailing.statistique.SentEmailEntity
/// The main  App

class EmailWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val apiKey = inputData.getString("api_key") ?: return Result.failure()
        val subject = inputData.getString("subject") ?: "Sans objet"
        val imageUriString = inputData.getString("image_uri") ?: ""
        val campaignId = inputData.getInt("campaign_id", -1)

        if (campaignId == -1) return Result.failure()

        val database = AppDatabase.getDatabase(applicationContext)
        val contactDao = database.contactDao()
        val campaignDao = database.campaignDao()
        val sentEmailDao = database.sentEmailDao()

        // 1. Fetch Campaign and Product details
        val fullDetails = campaignDao.getCampaignFullDetailsById(campaignId)
        val product = fullDetails?.product

        // 2. Get all contacts
        val allContacts = contactDao.getAllContactsList()
        
        // 3. Get emails that were already successfully sent for this specific campaign
        val alreadySentEmails = sentEmailDao.getEmailsSentForCampaign(campaignId).toSet()

        // 4. Filter contacts: only those who haven't received THIS campaign yet
        val recipients = allContacts.filter { it.email !in alreadySentEmails }
        
        Log.d("MailingFlow", "Total contacts: ${allContacts.size}, Already sent: ${alreadySentEmails.size}, To send now: ${recipients.size}")

        if (recipients.isEmpty()) {
            campaignDao.updateStatus(campaignId, "SENT")
            return Result.success()
        }

        // Prepare Image Attachment
        var attachments: MutableList<Attachment> = mutableListOf()
        val contentId = "campaign_image" 

        if (imageUriString.isNotEmpty()) {
            try {
                val uri = Uri.parse(imageUriString)
                val base64Image = SendGridUtils.encodeImageToBase64(applicationContext, uri)
                if (base64Image != null) {
                    val mimeType = SendGridUtils.getMimeType(applicationContext, uri)
                    attachments.add(
                        Attachment(
                            content = base64Image,
                            type = mimeType,
                            filename = "promotion_campagne",
                            disposition = "inline",
                            contentId = contentId
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e("EmailWorker", "Error processing image: ${e.message}")
            }
        }

        // Prepare Product Image Attachment if available
        val productContentId = "product_image"
        var hasProductImage = false
        if (product?.imageUri != null && product.imageUri.isNotEmpty()) {
            try {
                val pUri = Uri.parse(product.imageUri)
                val pBase64 = SendGridUtils.encodeImageToBase64(applicationContext, pUri)
                if (pBase64 != null) {
                    val pMimeType = SendGridUtils.getMimeType(applicationContext, pUri)
                    attachments.add(
                        Attachment(
                            content = pBase64,
                            type = pMimeType,
                            filename = "product_image",
                            disposition = "inline",
                            contentId = productContentId
                        )
                    )
                    hasProductImage = true
                }
            } catch (e: Exception) {
                Log.e("EmailWorker", "Error processing product image: ${e.message}")
            }
        }

        val productHtml = if (product != null) {
            """
            <div style="margin-top: 20px; padding: 20px; border: 1px solid #E0E0E0; border-radius: 8px; background-color: #F9F9F9;">
                <h2 style="color: #1A1A2E; margin-top: 0;">${product.name}</h2>
                ${if (hasProductImage) "<img src='cid:$productContentId' style='width: 100%; max-width: 200px; border-radius: 4px; margin-bottom: 10px;' />" else ""}
                <p style="color: #444444; font-size: 14px;">${product.description}</p>
                <div style="font-size: 18px; font-weight: bold; color: #2979FF; margin-top: 10px;">Prix: ${product.price} MAD</div>
                <div style="font-size: 12px; color: #888888; margin-top: 5px;">Catégorie: ${product.category}</div>
            </div>
            """.trimIndent()
        } else ""

        val htmlContent = """
            <!DOCTYPE html>
            <html>
            <body style="margin:0; padding:0; background-color: #E8EEF2; font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif;">
                <table width="100%" border="0" cellspacing="0" cellpadding="0" style="padding: 30px 10px;">
                    <tr>
                        <td align="center">
                            <table width="100%" style="max-width: 600px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0,0,0,0.1);">
                                <tr>
                                    <td style="background-color: #2979FF; padding: 15px 20px; text-align: center;">
                                        <span style="color: #ffffff; font-size: 16px; font-weight: bold; letter-spacing: 2px;">OFFRES EXCLUSIVES TOP SOFTWARE</span>
                                    </td>
                                </tr>
                                ${if (imageUriString.isNotEmpty()) """
                                <tr>
                                    <td style="padding: 0; background-color: #ffffff; text-align: center;">
                                        <img src="cid:$contentId" alt="Image Promo" style="width: 100%; max-width: 600px; height: auto; display: block; margin: 0; border-bottom: 4px solid #2979FF;" />
                                    </td>
                                </tr>
                                """ else ""}
                                <tr>
                                    <td style="padding: 40px 30px; text-align: center;">
                                        <h1 style="color: #1A1A2E; margin: 0 0 15px 0; font-size: 28px; font-weight: 900;">$subject</h1>
                                        <p style="color: #555555; font-size: 16px; line-height: 1.6; margin: 0 0 30px 0;">
                                            Découvrez notre sélection spéciale pour vous aujourd'hui !
                                        </p>
                                        
                                        $productHtml

                                        <div style="margin-top: 30px;">
                                            <a href="http://www.topsoftware.ma/" style="background-color: #2979FF; color: #ffffff; padding: 16px 45px; text-decoration: none; border-radius: 30px; font-weight: bold; font-size: 18px; display: inline-block;">
                                                Voir toutes les offres
                                            </a>
                                        </div>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="background-color: #F8F9FA; padding: 25px; text-align: center; border-top: 1px solid #EEEEEE;">
                                        <p style="color: #999999; font-size: 12px; margin: 0; line-height: 1.5;">
                                            © 2026 Top Software - Tanger, Maroc<br/>
                                            Cet e-mail vous a été envoyé car vous êtes inscrit à notre liste.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
        """.trimIndent()

        val service = RetrofitClient.instance
        val token = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        var totalSuccessful = 0

        for (contact in recipients) {
            val emailBody = SendGridModel(
                personalizations = listOf(Personalization(to = listOf(Recipient(email = contact.email)))),
                from = Recipient(email = "mehdielmoussaoui25@gmail.com"),
                subject = subject,
                content = listOf(Content(type = "text/html", value = htmlContent)),
                attachments = if (attachments.isNotEmpty()) attachments else null
            )

            try {
                val response = service.sendEmail(token, emailBody)
                val isSuccess = response.isSuccessful

                if (isSuccess) {
                    totalSuccessful++
                    contactDao.updateStatus(contact.id, "SENT")
                } else {
                    contactDao.updateStatus(contact.id, "FAILED")
                }

                // Insert into history (Statistiques)
                sentEmailDao.insertSentEmail(
                    SentEmailEntity(
                        campaignId = campaignId,
                        recipientEmail = contact.email,
                        subject = subject,
                        status = if (isSuccess) "SUCCESS" else "FAILED"
                    )
                )

            } catch (e: Exception) {
                contactDao.updateStatus(contact.id, "FAILED")
                sentEmailDao.insertSentEmail(
                    SentEmailEntity(
                        campaignId = campaignId,
                        recipientEmail = contact.email,
                        subject = subject,
                        status = "FAILED"
                    )
                )
            }
        }

        campaignDao.updateStatus(campaignId, "SENT")
        return Result.success()
    }
}
