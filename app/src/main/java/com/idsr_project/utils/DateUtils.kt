package com.idsr_project.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateUtils {

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy")
            .withZone(ZoneId.systemDefault())

    private val dateTimeFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a")
            .withZone(ZoneId.systemDefault())

    fun formatIsoDate(isoDate: String?): String {
        return try {
            if (isoDate.isNullOrEmpty()) {
                "N/A"
            } else {
                val instant = Instant.parse(isoDate)
                dateFormatter.format(instant)
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun formatIsoDateTime(isoDate: String?): String {
        return try {
            if (isoDate.isNullOrEmpty()) {
                "N/A"
            } else {
                val instant = Instant.parse(isoDate)
                dateTimeFormatter.format(instant)
            }
        } catch (e: Exception) {
            "N/A"
        }
    }

}