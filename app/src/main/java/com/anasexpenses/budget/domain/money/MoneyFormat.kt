package com.anasexpenses.budget.domain.money

import java.util.Locale

fun formatJodFromMilli(milli: Long): String =
    String.format(Locale.US, "%.3f", milli / 1000.0)
