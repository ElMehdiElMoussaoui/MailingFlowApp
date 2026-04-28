package com.example.appmailing.fct_send

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface SendGridService {
    @POST("v3/mail/send")
    suspend fun sendEmail(
        @Header("Authorization") token: String,
        @Body body: SendGridModel
    ): Response<Unit>
}