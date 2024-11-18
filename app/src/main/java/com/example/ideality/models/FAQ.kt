package com.example.ideality.models

data class FAQSection(
    val title: String,
    val items: List<FAQItem>
)

data class FAQItem(
    val question: String,
    val answer: String,
    var isExpanded: Boolean = false
)