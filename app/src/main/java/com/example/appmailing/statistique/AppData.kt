package com.example.appmailing

data class MailTemplate(
    val name: String,
    val subject: String,
    val content: String
)

data class Campaign(
    val title: String,
    val status: String,
    val recipientCount: Int,
    val timestamp: String
)

object AppData {
    var totalEmailsSent = 0

    val savedTemplates = mutableListOf(
        MailTemplate(
            name = "Welcome Series",
            subject = "Welcome to MailFlow!",
            content = "<h2>Welcome!</h2><p>Thank you for joining us. We are excited to have you on board.</p>"
        ),
        MailTemplate(
            name = "Summer Flash Sale",
            subject = "48hr Flash Sale — 30% off everything",
            content = "<h2>Summer Sale!</h2><p>Don't miss our biggest sale of the year. Use code SUMMER30.</p>"
        ),
        MailTemplate(
            name = "Monthly Newsletter",
            subject = "Your November Newsletter",
            content = "<h2>November Updates</h2><p>Here are this month's highlights and news.</p>"
        ),
        MailTemplate(
            name = "Cart Reminder",
            subject = "You left something behind...",
            content = "<h2>Complete your purchase</h2><p>You have items waiting in your cart.</p>"
        )
    )

    val campaignHistory = mutableListOf(
        Campaign("Summer Flash Sale 2024", "SENT", 12450, "Oct 24, 2023 · 10:30 AM"),
        Campaign("Monthly Newsletter — November", "PENDING", 8200, "Scheduled for Nov 01"),
        Campaign("Abandoned Cart Reminder", "FAILED", 432, "Oct 22, 2023 · 02:15 PM"),
        Campaign("Welcome Series: New Users", "SENT", 2100, "Oct 20, 2023 · 09:00 AM"),
        Campaign("Black Friday Teaser", "SCHEDULED", 15000, "Nov 15, 2023 · 08:00 AM")
    )
}