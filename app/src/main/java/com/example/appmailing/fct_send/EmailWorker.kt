package com.example.appmailing.fct_send

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.appmailing.TheContact.SendingStatus
import com.example.appmailing.statistique.AppDatabase
import com.example.appmailing.statistique.SentEmailEntity

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

        val fullDetails = campaignDao.getCampaignFullDetailsById(campaignId)
        val product = fullDetails?.product

        val allContacts = contactDao.getAllContactsList()
        val alreadySentEmails = sentEmailDao.getEmailsSentForCampaign(campaignId).toSet()

        // Filter: only PENDING or FAILED contacts who haven't received THIS campaign successfully yet
        val recipients = allContacts.filter { it.email !in alreadySentEmails }
        
        Log.d("EmailWorker", "Total contacts: ${allContacts.size}, To send now: ${recipients.size}")

        if (recipients.isEmpty()) {
            campaignDao.updateStatus(campaignId, "SENT")
            return Result.success(workDataOf("success" to 0, "failed" to 0, "total" to 0))
        }

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

        // ── Product card block (injected only if product != null) ──


        val productBlock = if (product != null)
            """
    <!-- PRODUCT CARD -->
    <table width="100%" cellpadding="0" cellspacing="0" style="margin-top: 20px;">
      <tr>
        <td style="background-color: #F0F4FF; background: linear-gradient(135deg, #F0F4FF, #E8EEFF);
                    border: 1px solid #C8D4FF; border-radius: 14px; padding: 15px;">
          <table width="100%" cellpadding="0" cellspacing="0">
            <tr>
              <td width="80" valign="top" style="padding-right: 12px;">
                ${if (hasProductImage) """
                <img src="cid:$productContentId"
                     width="80" height="80"
                     style="border-radius: 10px; object-fit: cover;
                            border: 1px solid #C2CEFF; display: block;" />
                """ else """
                <table width="80" height="80" cellpadding="0" cellspacing="0">
                  <tr><td align="center" valign="middle"
                      style="background-color: #C7D7FF; background: linear-gradient(135deg, #C7D7FF, #E0E8FF);
                             border-radius: 10px; border: 1px solid #C2CEFF;">
                    <span style="font-size: 28px;">📦</span>
                  </td></tr>
                </table>
                """}
              </td>
              <td valign="top">
                <p style="margin: 0 0 4px; font-size: 10px; font-weight: 700;
                           letter-spacing: 0.1em; text-transform: uppercase;
                           color: #5B8BFF; font-family: Helvetica, Arial, sans-serif;">
                  ${product.category}
                </p>
                <h3 style="margin: 0 0 6px; font-size: 17px; font-weight: 800;
                            color: #0D0F1A; font-family: Helvetica, Arial, sans-serif; line-height: 1.2;">
                  ${product.name}
                </h3>
                <p style="margin: 0 0 10px; font-size: 13px; color: #555555;
                           line-height: 1.4; font-family: Helvetica, Arial, sans-serif; word-break: break-word;">
                  ${product.description}
                </p>
                <table cellpadding="0" cellspacing="0">
                  <tr>
                    <td valign="middle" style="font-size: 17px; font-weight: 800; color: #1A56FF;
                               font-family: Helvetica, Arial, sans-serif; white-space: nowrap;">
                      ${product.price} ${product.currency}
                    </td>
                    <td width="8"></td>
                    <td valign="middle" style="background-color: #FF3B5C; background: linear-gradient(135deg, #FF3B5C, #FF6B6B);
                               color: white; font-size: 10px; font-weight: 700;
                               padding: 4px 8px; border-radius: 12px;
                               font-family: Helvetica, Arial, sans-serif; letter-spacing: 0.05em; white-space: nowrap;">
                      PROMO
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
    """.trimIndent() else ""

        // ── Hero image row (only if user picked a promo image) ──
        val heroImageBlock = if (imageUriString.isNotEmpty()) """
            <tr>
              <td style="padding:0;background:#ffffff;text-align:center;">
                <img src="cid:$contentId" alt="Image Promo"
                     style="width:100%;max-width:600px;height:240px;
                            object-fit:cover;display:block;
                            border-bottom:4px solid #1A56FF;" />
              </td>
            </tr>
        """.trimIndent() else ""

        // ── Full HTML email template ──
        val htmlContent = """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1">
              <meta name="x-apple-disable-message-reformatting">
              <title>$subject</title>
            </head>
            <body style="margin:0;padding:0;background-color:#EEF1F8;
                         font-family:'Helvetica Neue',Helvetica,Arial,sans-serif;
                         -webkit-text-size-adjust:100%;">

            <!-- WRAPPER -->
            <table width="100%" border="0" cellspacing="0" cellpadding="0"
                   style="background:#EEF1F8;padding:32px 12px;">
              <tr><td align="center">
              <table width="100%" style="max-width:600px;border-radius:20px;
                                          overflow:hidden;
                                          box-shadow:0 16px 48px rgba(0,0,0,0.12);">

                <!-- ══ HERO HEADER ══ -->
                <tr>
                  <td style="background:linear-gradient(135deg,#0A0F2E 0%,#0E1D5C 40%,
                                         #1535A0 70%,#1A56FF 100%);
                             padding:50px 36px 42px;text-align:center;position:relative;">

                    <!-- Logo -->
                    <p style="margin:0 0 24px;font-size:11px;font-weight:700;
                              letter-spacing:0.2em;color:rgba(255,255,255,0.35);
                              text-transform:uppercase;">
                      TOP SOFTWARE
                    </p>

                    <!-- Badge -->
                    <div style="display:inline-block;background:rgba(245,166,35,0.15);
                                border:1px solid rgba(245,166,35,0.4);
                                color:#F5A623;font-size:11px;font-weight:700;
                                letter-spacing:0.12em;text-transform:uppercase;
                                padding:5px 16px;border-radius:20px;margin-bottom:20px;">
                      ★ Offres Exclusives
                    </div>

                    <!-- Title -->
                    <h1 style="margin:0 0 14px;font-size:32px;font-weight:900;
                               color:#FFFFFF;line-height:1.15;letter-spacing:-0.5px;">
                      $subject
                    </h1>

                    <!-- Subtitle -->
                    <p style="margin:0;font-size:15px;color:rgba(255,255,255,0.70);
                              line-height:1.6;">
                      Découvrez notre sélection spéciale pour vous aujourd'hui !
                    </p>
                  </td>
                </tr>

                <!-- ══ PROMO IMAGE (optional) ══ -->
                $heroImageBlock

                <!-- ══ STATS STRIP ══ -->


                <!-- ══ PRODUCT BODY ══ -->
                <tr>
                  <td style="background:#FFFFFF;padding:36px 36px 10px;">

                    <!-- Section eyebrow -->
                    <p style="margin:0 0 20px;font-size:10px;font-weight:700;
                              letter-spacing:0.14em;text-transform:uppercase;color:#1A56FF;">
                      Produit recommandé
                    </p>

                    <!-- Product card -->
                    $productBlock

                  </td>
                </tr>

                <!-- ══ CTA ══ -->
                <tr>
                  <td style="background:#FFFFFF;padding:24px 36px 36px;text-align:center;">
                    <a href="http://www.topsoftware.ma/"
                       style="display:inline-block;
                              background:linear-gradient(135deg,#1535A0,#1A56FF,#4A7BFF);
                              color:#ffffff;text-decoration:none;
                              font-size:15px;font-weight:700;letter-spacing:0.04em;
                              padding:17px 52px;border-radius:50px;
                              box-shadow:0 8px 28px rgba(26,86,255,0.40);">
                      Voir toutes les offres →
                    </a>

                  </td>
                </tr>

                <!-- ══ FEATURES ROW ══ -->
                <tr>
                  <td style="background:#FAFBFF;border-top:1px solid #E8EAF0;
                             border-bottom:1px solid #E8EAF0;padding:0;">
                    <table width="100%" cellpadding="0" cellspacing="0">
                      <tr>
                        <td width="33%" style="padding:18px 12px;text-align:center;
                                               border-right:1px solid #E8EAF0;">
                          <p style="margin:0 0 6px;font-size:20px;">⭐</p>
                          <p style="margin:0;font-size:12px;font-weight:600;color:#0D0F1A;">Qualité Premium</p>
                          
                        </td>
                        <td width="33%" style="padding:18px 12px;text-align:center;
                                               border-right:1px solid #E8EAF0;">
                          <p style="margin:0 0 6px;font-size:20px;">🔒</p>
                          <p style="margin:0;font-size:12px;font-weight:600;color:#0D0F1A;">Paiement Sécurisé</p>
                          
                        </td>
                        <td width="33%" style="padding:18px 12px;text-align:center;">
                          <p style="margin:0 0 6px;font-size:20px;">💬</p>
                          <p style="margin:0;font-size:12px;font-weight:600;color:#0D0F1A;">Support 7j/7</p>
                        
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>

                <!-- ══ FOOTER ══ -->
                <tr>
                  <td style="background:#0D0F1A;padding:30px 36px;text-align:center;">
                    <p style="margin:0 0 10px;font-size:18px;font-weight:900;
                              color:#FFFFFF;letter-spacing:0.05em;">
                      TOP <span style="color:#1A56FF">SOFTWARE</span>
                    </p>
                    <p style="margin:0 0 14px;font-size:11px;color:rgba(255,255,255,0.4);line-height:1.7;">
                      Tanger, Maroc · www.topsoftware.ma
                    </p>
                    <p style="margin:0;font-size:11px;color:rgba(255,255,255,0.25);line-height:1.7;">
                      © 2026 Top Software – Tous droits réservés.<br>
                      Cet e-mail vous a été envoyé car vous êtes inscrit à notre liste.<br>
                      <a href="#" style="color:rgba(255,255,255,0.4);text-decoration:underline;">
                        Se désabonner
                      </a>
                    </p>
                  </td>
                </tr>

              </table>
              </td></tr>
            </table>

            </body>
            </html>
        """.trimIndent()

        val service = RetrofitClient.instance
        val token = if (apiKey.startsWith("Bearer ")) apiKey else "Bearer $apiKey"
        var totalSuccessful = 0
        var totalFailed = 0

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
                    contactDao.updateStatus(contact.id, SendingStatus.SENT.name)
                } else {
                    totalFailed++
                    val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                    Log.e("EmailWorker", "SendGrid error for ${contact.email}: $errorMsg")
                    contactDao.updateStatus(contact.id, SendingStatus.FAILED.name)
                }

                sentEmailDao.insertSentEmail(
                    SentEmailEntity(
                        campaignId = campaignId,
                        recipientEmail = contact.email,
                        subject = subject,
                        status = if (isSuccess) "SUCCESS" else "FAILED"
                    )
                )

            } catch (e: Exception) {
                totalFailed++
                Log.e("EmailWorker", "Exception sending to ${contact.email}: ${e.message}")
                contactDao.updateStatus(contact.id, SendingStatus.FAILED.name)
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
        
        val outputData = workDataOf(
            "success" to totalSuccessful,
            "failed" to totalFailed,
            "total" to recipients.size
        )
        return Result.success(outputData)
    }
}
