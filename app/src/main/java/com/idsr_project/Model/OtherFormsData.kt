package com.idsr_project.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class OtherFormsData(
    val id: Int,
    val type: String,
    val title: String,
    val subTitle: String,
    val date: String,
    val rawData: Parcelable
) : Parcelable