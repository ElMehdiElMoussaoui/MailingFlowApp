package com.example.appmailing.fct_send

import com.google.gson.annotations.SerializedName

data class SendGridModel(
    @SerializedName("personalizations") val personalizations: List<Personalization>,
    @SerializedName("from") val from: Recipient,
    @SerializedName("subject") val subject: String,
    @SerializedName("content") val content: List<Content>,
    @SerializedName("attachments") val attachments: List<Attachment>? = null
)

data class Personalization(
    @SerializedName("to") val to: List<Recipient>
)

data class Recipient(
    @SerializedName("email") val email: String,
    @SerializedName("name") val name: String? = null
)

data class Content(
    @SerializedName("type") val type: String = "text/html",
    @SerializedName("value") val value: String
)

data class Attachment(
    @SerializedName("content") val content: String, // Base64 encoded content
    @SerializedName("type") val type: String,       // e.g., "image/png"
    @SerializedName("filename") val filename: String,
    @SerializedName("disposition") val disposition: String = "attachment", // "attachment" or "inline"
    @SerializedName("content_id") val contentId: String? = null // Used if disposition is "inline"
)
