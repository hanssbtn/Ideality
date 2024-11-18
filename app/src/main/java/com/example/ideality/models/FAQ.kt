package com.example.ideality.models

data class FAQSection(
    val title: String,
    val items: List<FAQItem>
)

data class FAQItem(
    val question: String,
    val answer: String,
    val isExpanded: Boolean = false
)