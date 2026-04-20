package com.idsr_project.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import com.idsr_project.Model.Disease
import com.idsr_project.Model.FormData
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.idsr_project.Model.Annex2FData
import com.idsr_project.Model.Annex2GData
import com.idsr_project.Model.LabReportData

object ExportManager {

    private val timestamp: String
        get() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

    private val PRIMARY   = "#005BAC".toColorInt()
    private val LIGHT_BG  = "#F0F6FF".toColorInt()
    private val GRAY_LINE = "#E0E0E0".toColorInt()


    // Surveillance Export
    fun exportSurveillancePdf(context: Context, f: FormData) {
        val document   = PdfDocument()
        val pageWidth  = 595
        val pageHeight = 842
        val margin     = 40f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page     = document.startPage(pageInfo)
        val canvas   = page.canvas

        val paintTitle = Paint().apply { color = PRIMARY;       textSize = 18f; isFakeBoldText = true }
        val paintSub   = Paint().apply { color = Color.GRAY;    textSize = 10f }
        val paintLabel = Paint().apply { color = PRIMARY;       textSize = 10f; isFakeBoldText = true }
        val paintValue = Paint().apply { color = Color.DKGRAY;  textSize = 10f }
        val paintLine  = Paint().apply { color = GRAY_LINE;     strokeWidth = 1f }
        val paintBg    = Paint().apply { color = LIGHT_BG }
        val paintDiseaseHeader = Paint().apply { color = Color.WHITE; textSize = 9f; isFakeBoldText = true }
        val paintDiseaseCell   = Paint().apply { color = Color.DKGRAY; textSize = 9f }

        var y = margin


        canvas.drawText("IDSR Surveillance Report", margin, y + 20f, paintTitle)
        y += 28f
        canvas.drawText(
            "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}",
            margin, y + 10f, paintSub
        )
        y += 24f
        canvas.drawLine(margin, y, pageWidth - margin, y, paintLine)
        y += 16f

        fun drawRow(label: String, value: String) {
            canvas.drawRect(margin, y, pageWidth - margin, y + 22f, paintBg)
            canvas.drawText(label, margin + 6f, y + 15f, paintLabel)
            canvas.drawText(value, margin + 160f, y + 15f, paintValue)
            canvas.drawLine(margin, y + 22f, pageWidth - margin, y + 22f, paintLine)
        }

        fun drawSectionTitle(title: String) {
            val bgPaint = Paint().apply { color = PRIMARY }
            canvas.drawRect(margin, y, pageWidth - margin, y + 24f, bgPaint)
            val wp = Paint().apply { color = Color.WHITE; textSize = 11f; isFakeBoldText = true }
            canvas.drawText(title, margin + 8f, y + 17f, wp)
        }


        drawSectionTitle("Facility Information")
        var localY = y + 24f; val tempY1 = localY


        fun row(label: String, value: String?) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 22f, paintBg)
            canvas.drawText(label, margin + 6f, localY + 15f, paintLabel)
            canvas.drawText(value ?: "N/A", margin + 160f, localY + 15f, paintValue)
            canvas.drawLine(margin, localY + 22f, pageWidth - margin, localY + 22f, paintLine)
            localY += 22f
        }

        row("Facility",    f.facility_name)
        row("Region",      f.region_name)
        row("District",    f.district_name)
        row("Coordinates", f.facility_geo)
        y = localY + 12f


        drawSectionTitle("Report Period")
        localY = y + 24f

        row("Epiweek",   f.epiweek)
        row("Date From", DateUtils.formatIsoDate(f.date_from))
        row("Date To",   DateUtils.formatIsoDate(f.date_to))
        y = localY + 12f


        drawSectionTitle("Consultation Totals")
        localY = y + 24f

        row("Under 5 Male",   f.tot_con_u5_male?.toString())
        row("Under 5 Female", f.tot_con_u5_female?.toString())
        row("Above 5 Male",   f.tot_con_a5_male?.toString())
        row("Above 5 Female", f.tot_con_a5_female?.toString())
        row("Grand Total",    f.grand_total?.toString())
        y = localY + 12f


        drawSectionTitle("Officer Details")
        localY = y + 24f

        row("Officer Name", f.officer_name)
        row("Designation",  f.designation)
        row("Comment",      f.officer_comment)
        row("Submitted At", DateUtils.formatIsoDateTime(f.created_at))
        y = localY + 16f


        val diseases = f.diseases ?: emptyList()
        if (diseases.isNotEmpty()) {
            drawSectionTitle("Disease Breakdown")
            localY = y + 24f


            val diseaseCols  = listOf("Disease", "U5 Male", "U5 Female", "5+ Male", "5+ Female", "Total")
            val diseaseWidths = listOf(155f, 60f, 65f, 60f, 65f, 50f)

            val headerBg = Paint().apply { color = "#005BAC".toColorInt() }
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 22f, headerBg)

            var xPos = margin + 4f
            diseaseCols.forEachIndexed { i, col ->
                canvas.drawText(col, xPos, localY + 15f, paintDiseaseHeader)
                xPos += diseaseWidths[i]
            }
            localY += 22f

            diseases.forEachIndexed { idx, d ->
                if (idx % 2 == 0) {
                    canvas.drawRect(margin, localY, pageWidth - margin, localY + 20f, paintBg)
                }
                xPos = margin + 4f
                val cells = listOf(
                    d.name           ?: "",
                    (d.under5_male   ?: 0).toString(),
                    (d.under5_female ?: 0).toString(),
                    (d.above5_male   ?: 0).toString(),
                    (d.above5_female ?: 0).toString(),
                    (d.total         ?: 0).toString()
                )
                cells.forEachIndexed { i, cell ->
                    canvas.drawText(cell, xPos, localY + 14f, paintDiseaseCell)
                    xPos += diseaseWidths[i]
                }
                canvas.drawLine(margin, localY + 20f, pageWidth - margin, localY + 20f, paintLine)
                localY += 20f
            }
        }

        document.finishPage(page)
        saveDocumentAndShare(context, document, "surveillance_${f.id}_$timestamp.pdf")
    }

    fun exportSurveillanceCsv(context: Context, f: FormData) {
        val sb = StringBuilder()


        sb.appendLine("IDSR Surveillance Report")
        sb.appendLine("Generated,${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine()
        sb.appendLine("Field,Value")
        sb.appendLine("Facility,${f.facility_name ?: ""}")
        sb.appendLine("Region,${f.region_name ?: ""}")
        sb.appendLine("District,${f.district_name ?: ""}")
        sb.appendLine("Coordinates,${f.facility_geo ?: ""}")
        sb.appendLine("Epiweek,${f.epiweek ?: ""}")
        sb.appendLine("Date From,${DateUtils.formatIsoDate(f.date_from)}")
        sb.appendLine("Date To,${DateUtils.formatIsoDate(f.date_to)}")
        sb.appendLine("Under 5 Male,${f.tot_con_u5_male ?: 0}")
        sb.appendLine("Under 5 Female,${f.tot_con_u5_female ?: 0}")
        sb.appendLine("Above 5 Male,${f.tot_con_a5_male ?: 0}")
        sb.appendLine("Above 5 Female,${f.tot_con_a5_female ?: 0}")
        sb.appendLine("Grand Total,${f.grand_total ?: 0}")
        sb.appendLine("Officer Name,${f.officer_name ?: ""}")
        sb.appendLine("Designation,${f.designation ?: ""}")
        sb.appendLine("Comment,${f.officer_comment ?: ""}")
        sb.appendLine("Submitted At,${DateUtils.formatIsoDateTime(f.created_at)}")

        // Disease breakdown
        val diseases = f.diseases ?: emptyList()
        if (diseases.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Disease Breakdown")
            sb.appendLine("Disease,Under 5 Male,Under 5 Female,Above 5 Male,Above 5 Female,Total")
            diseases.forEach { d ->
                sb.appendLine(
                    "${d.name ?: ""},${d.under5_male ?: 0},${d.under5_female ?: 0}," +
                            "${d.above5_male ?: 0},${d.above5_female ?: 0},${d.total ?: 0}"
                )
            }
        }

        saveAndShare(context, sb.toString(), "surveillance_${f.id}_$timestamp.csv", "text/csv")
    }



    private fun saveAndShare(context: Context, content: String, fileName: String, mimeType: String) {
        try {
            val file = File(context.filesDir, fileName)  // use internal filesDir
            file.writeText(content)
            shareFile(context, file, mimeType)
        } catch (e: Exception) {
            android.util.Log.e("EXPORT", "Save failed: ${e.message}", e)
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveDocumentAndShare(context: Context, document: PdfDocument, fileName: String) {
        try {
            val file = File(context.filesDir, fileName)  // use internal filesDir
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()
            shareFile(context, file, "application/pdf")
        } catch (e: Exception) {
            android.util.Log.e("EXPORT", "PDF save failed: ${e.message}", e)
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Export via").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // add this to chooser too
        }


        val resInfoList = context.packageManager.queryIntentActivities(
            chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )
        resInfoList.forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(chooser)
    }

    // Annex2F Immediate Export
    fun exportAnnex2FPdf(context: Context, d: Annex2FData) {
        val document   = PdfDocument()
        val pageWidth  = 595
        val pageHeight = 842
        val margin     = 40f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page     = document.startPage(pageInfo)
        val canvas   = page.canvas

        val paintTitle  = Paint().apply { color = PRIMARY;      textSize = 18f; isFakeBoldText = true }
        val paintSub    = Paint().apply { color = Color.GRAY;   textSize = 10f }
        val paintLabel  = Paint().apply { color = PRIMARY;      textSize = 10f; isFakeBoldText = true }
        val paintValue  = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        val paintLine   = Paint().apply { color = GRAY_LINE;    strokeWidth = 1f }
        val paintBg     = Paint().apply { color = LIGHT_BG }
        val paintSecBg  = Paint().apply { color = PRIMARY }
        val paintSecTxt = Paint().apply { color = Color.WHITE;  textSize = 11f; isFakeBoldText = true }

        var localY = margin

        canvas.drawText("IDSR Annex2F Immediate Case Report", margin, localY + 20f, paintTitle)
        localY += 28f
        canvas.drawText(
            "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}",
            margin, localY + 10f, paintSub
        )
        localY += 24f
        canvas.drawLine(margin, localY, pageWidth - margin, localY, paintLine)
        localY += 16f

        fun sectionTitle(title: String) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 24f, paintSecBg)
            canvas.drawText(title, margin + 8f, localY + 17f, paintSecTxt)
            localY += 24f
        }

        fun row(label: String, value: String?) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 22f, paintBg)
            canvas.drawText(label, margin + 6f, localY + 15f, paintLabel)
            canvas.drawText(value ?: "N/A", margin + 180f, localY + 15f, paintValue)
            canvas.drawLine(margin, localY + 22f, pageWidth - margin, localY + 22f, paintLine)
            localY += 22f
        }

        sectionTitle("Patient Information")
        row("Patient Name",    d.patientName)
        row("Age",             d.age)
        row("Gender",          d.gender)
        row("Phone",           d.phoneNumber)
        row("Address",         d.address)
        row("Occupation",      d.occupation)
        row("Urban / Rural",   d.urbanRural)
        localY += 10f

        sectionTitle("Case Details")
        row("Disease",             d.disease)
        row("Site",                d.site)
        row("Inpatient/Outpatient",d.inpatientOutpatient)
        row("Date Seen",           DateUtils.formatIsoDate(d.dateSeen))
        row("Date of Onset",       DateUtils.formatIsoDate(d.dateOfOnset))
        row("Travel History",      d.travelHistory)
        row("Destination",         d.destination)
        row("Outcome",             d.outcome)
        row("Classification",      d.classification)
        localY += 10f

        sectionTitle("Vaccination & Lab")
        row("Vaccine Doses",       d.vaccineDoses)
        row("Date Last Vaccine",   DateUtils.formatIsoDate(d.dateLastVaccine))
        row("Date Specimen",       DateUtils.formatIsoDate(d.dateSpecimen))
        row("Date Lab",            DateUtils.formatIsoDate(d.dateLab))
        row("Lab Results",         d.labResults)
        localY += 10f

        sectionTitle("Reporting Info")
        row("Region",                  d.region_name)
        row("District",                d.district_name)
        row("Date Facility Notified",  DateUtils.formatIsoDate(d.dateFacilityNotified))
        row("Date Sent to District",   DateUtils.formatIsoDate(d.dateSentDistrict))
        row("Reporter Name",           d.reporterName)
        row("Submitted By",            d.full_name)
        row("Submitted At",            DateUtils.formatIsoDateTime(d.created_at))

        document.finishPage(page)
        saveDocumentAndShare(context, document, "annex2f_${d.id}_$timestamp.pdf")
    }

    fun exportAnnex2FCsv(context: Context, d: Annex2FData) {
        val sb = StringBuilder()
        sb.appendLine("IDSR Annex2F Immediate Case Report")
        sb.appendLine("Generated,${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine()
        sb.appendLine("Field,Value")
        sb.appendLine("Patient Name,${d.patientName ?: ""}")
        sb.appendLine("Age,${d.age ?: ""}")
        sb.appendLine("Gender,${d.gender ?: ""}")
        sb.appendLine("Phone,${d.phoneNumber ?: ""}")
        sb.appendLine("Address,${d.address ?: ""}")
        sb.appendLine("Occupation,${d.occupation ?: ""}")
        sb.appendLine("Urban/Rural,${d.urbanRural ?: ""}")
        sb.appendLine("Disease,${d.disease ?: ""}")
        sb.appendLine("Site,${d.site ?: ""}")
        sb.appendLine("Inpatient/Outpatient,${d.inpatientOutpatient ?: ""}")
        sb.appendLine("Date Seen,${DateUtils.formatIsoDate(d.dateSeen)}")
        sb.appendLine("Date of Onset,${DateUtils.formatIsoDate(d.dateOfOnset)}")
        sb.appendLine("Travel History,${d.travelHistory ?: ""}")
        sb.appendLine("Destination,${d.destination ?: ""}")
        sb.appendLine("Outcome,${d.outcome ?: ""}")
        sb.appendLine("Classification,${d.classification ?: ""}")
        sb.appendLine("Vaccine Doses,${d.vaccineDoses ?: ""}")
        sb.appendLine("Date Last Vaccine,${DateUtils.formatIsoDate(d.dateLastVaccine)}")
        sb.appendLine("Date Specimen,${DateUtils.formatIsoDate(d.dateSpecimen)}")
        sb.appendLine("Date Lab,${DateUtils.formatIsoDate(d.dateLab)}")
        sb.appendLine("Lab Results,${d.labResults ?: ""}")
        sb.appendLine("Region,${d.region_name ?: ""}")
        sb.appendLine("District,${d.district_name ?: ""}")
        sb.appendLine("Date Facility Notified,${DateUtils.formatIsoDate(d.dateFacilityNotified)}")
        sb.appendLine("Date Sent to District,${DateUtils.formatIsoDate(d.dateSentDistrict)}")
        sb.appendLine("Reporter Name,${d.reporterName ?: ""}")
        sb.appendLine("Submitted By,${d.full_name ?: ""}")
        sb.appendLine("Submitted At,${DateUtils.formatIsoDateTime(d.created_at)}")

        saveAndShare(context, sb.toString(), "annex2f_${d.id}_$timestamp.csv", "text/csv")
    }

    // Annex2G Specimen Report
    fun exportAnnex2GPdf(context: Context, d: Annex2GData) {
        val document   = PdfDocument()
        val pageWidth  = 595
        val pageHeight = 842
        val margin     = 40f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page     = document.startPage(pageInfo)
        val canvas   = page.canvas

        val paintTitle  = Paint().apply { color = PRIMARY;      textSize = 18f; isFakeBoldText = true }
        val paintSub    = Paint().apply { color = Color.GRAY;   textSize = 10f }
        val paintLabel  = Paint().apply { color = PRIMARY;      textSize = 10f; isFakeBoldText = true }
        val paintValue  = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        val paintLine   = Paint().apply { color = GRAY_LINE;    strokeWidth = 1f }
        val paintBg     = Paint().apply { color = LIGHT_BG }
        val paintSecBg  = Paint().apply { color = PRIMARY }
        val paintSecTxt = Paint().apply { color = Color.WHITE;  textSize = 11f; isFakeBoldText = true }

        var localY = margin

        canvas.drawText("IDSR Annex2G Lab Report (Specimen)", margin, localY + 20f, paintTitle)
        localY += 28f
        canvas.drawText(
            "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}",
            margin, localY + 10f, paintSub
        )
        localY += 24f
        canvas.drawLine(margin, localY, pageWidth - margin, localY, paintLine)
        localY += 16f

        fun sectionTitle(title: String) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 24f, paintSecBg)
            canvas.drawText(title, margin + 8f, localY + 17f, paintSecTxt)
            localY += 24f
        }

        fun row(label: String, value: String?) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 22f, paintBg)
            canvas.drawText(label, margin + 6f, localY + 15f, paintLabel)
            canvas.drawText(value ?: "N/A", margin + 180f, localY + 15f, paintValue)
            canvas.drawLine(margin, localY + 22f, pageWidth - margin, localY + 22f, paintLine)
            localY += 22f
        }

        sectionTitle("Patient Information")
        row("Patient Name", d.patientNameLab)
        row("Gender",       d.sex)
        row("Age",          d.age)
        row("Phone",        d.phoneNumber)
        localY += 10f

        sectionTitle("Specimen Details")
        row("Specimen ID",           d.specimenUniqueID)
        row("Specimen Type",         d.specimenType)
        row("Suspected Disease",     d.suspectedDisease)
        row("Date Specimen Collect", DateUtils.formatIsoDate(d.dateSpecimenCollect))
        row("Date Sent to Lab",      DateUtils.formatIsoDate(d.dateSpecimenSentLab))
        localY += 10f

        sectionTitle("Clinician & Location")
        row("Clinician Email", d.emailClinician)
        row("Region",          d.region_name)
        row("District",        d.district_name)
        row("Submitted By",    d.full_name)
        row("Submitted At",    DateUtils.formatIsoDateTime(d.created_at))

        document.finishPage(page)
        saveDocumentAndShare(context, document, "annex2g_${d.id}_$timestamp.pdf")
    }

    fun exportAnnex2GCsv(context: Context, d: Annex2GData) {
        val sb = StringBuilder()
        sb.appendLine("IDSR Annex2G Lab Report (Specimen)")
        sb.appendLine("Generated,${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine()
        sb.appendLine("Field,Value")
        sb.appendLine("Patient Name,${d.patientNameLab ?: ""}")
        sb.appendLine("Gender,${d.sex ?: ""}")
        sb.appendLine("Age,${d.age ?: ""}")
        sb.appendLine("Phone,${d.phoneNumber ?: ""}")
        sb.appendLine("Specimen ID,${d.specimenUniqueID ?: ""}")
        sb.appendLine("Specimen Type,${d.specimenType ?: ""}")
        sb.appendLine("Suspected Disease,${d.suspectedDisease ?: ""}")
        sb.appendLine("Date Specimen Collect,${DateUtils.formatIsoDate(d.dateSpecimenCollect)}")
        sb.appendLine("Date Sent to Lab,${DateUtils.formatIsoDate(d.dateSpecimenSentLab)}")
        sb.appendLine("Clinician Email,${d.emailClinician ?: ""}")
        sb.appendLine("Region,${d.region_name ?: ""}")
        sb.appendLine("District,${d.district_name ?: ""}")
        sb.appendLine("Submitted By,${d.full_name ?: ""}")
        sb.appendLine("Submitted At,${DateUtils.formatIsoDateTime(d.created_at)}")

        saveAndShare(context, sb.toString(), "annex2g_${d.id}_$timestamp.csv", "text/csv")
    }

    // Laboratory Report
    fun exportLabReportPdf(context: Context, d: LabReportData) {
        val document   = PdfDocument()
        val pageWidth  = 595
        val pageHeight = 842
        val margin     = 40f

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page     = document.startPage(pageInfo)
        val canvas   = page.canvas

        val paintTitle  = Paint().apply { color = PRIMARY;      textSize = 18f; isFakeBoldText = true }
        val paintSub    = Paint().apply { color = Color.GRAY;   textSize = 10f }
        val paintLabel  = Paint().apply { color = PRIMARY;      textSize = 10f; isFakeBoldText = true }
        val paintValue  = Paint().apply { color = Color.DKGRAY; textSize = 10f }
        val paintLine   = Paint().apply { color = GRAY_LINE;    strokeWidth = 1f }
        val paintBg     = Paint().apply { color = LIGHT_BG }
        val paintSecBg  = Paint().apply { color = PRIMARY }
        val paintSecTxt = Paint().apply { color = Color.WHITE;  textSize = 11f; isFakeBoldText = true }

        // Result color
        val resultColor = when (d.final_lab_result?.lowercase()) {
            "positive" -> Color.parseColor("#C62828")
            "negative" -> Color.parseColor("#2E7D32")
            else       -> Color.GRAY
        }
        val paintResult = Paint().apply { color = resultColor; textSize = 10f; isFakeBoldText = true }

        var localY = margin

        canvas.drawText("IDSR Lab Report (Final Result)", margin, localY + 20f, paintTitle)
        localY += 28f
        canvas.drawText(
            "Generated: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())}",
            margin, localY + 10f, paintSub
        )
        localY += 24f
        canvas.drawLine(margin, localY, pageWidth - margin, localY, paintLine)
        localY += 16f

        fun sectionTitle(title: String) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 24f, paintSecBg)
            canvas.drawText(title, margin + 8f, localY + 17f, paintSecTxt)
            localY += 24f
        }

        fun row(label: String, value: String?, highlight: Boolean = false) {
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 22f, paintBg)
            canvas.drawText(label, margin + 6f, localY + 15f, paintLabel)
            canvas.drawText(value ?: "N/A", margin + 180f, localY + 15f,
                if (highlight) paintResult else paintValue)
            canvas.drawLine(margin, localY + 22f, pageWidth - margin, localY + 22f, paintLine)
            localY += 22f
        }

        sectionTitle("Lab Information")
        row("Lab Name",           d.lab_name)
        row("Date Lab Received",  DateUtils.formatIsoDate(d.date_lab_received))
        row("Specimen Condition", d.specimen_condition)
        row("Test Types",         d.test_types_performed)
        localY += 10f

        sectionTitle("Final Result")
        row("Final Lab Result",   d.final_lab_result, highlight = true)
        localY += 10f

        sectionTitle("Administrative Info")
        row("Date Sent to District",       DateUtils.formatIsoDate(d.date_lab_sent_district))
        row("Date District Received",      DateUtils.formatIsoDate(d.date_district_received_lab_result))
        row("Region",                      d.region_name)
        row("District",                    d.district_name)
        row("Submitted By",                d.full_name)
        row("Submitted At",                DateUtils.formatIsoDateTime(d.created_at))
        localY += 10f

        // Images note
        val imageCount = d.lab_result_images?.size ?: 0
        if (imageCount > 0) {
            sectionTitle("Lab Result Images")
            canvas.drawRect(margin, localY, pageWidth - margin, localY + 22f, paintBg)
            canvas.drawText(
                "$imageCount image(s) attached — view in app",
                margin + 6f, localY + 15f, paintValue
            )
            localY += 22f
        }

        document.finishPage(page)
        saveDocumentAndShare(context, document, "labreport_${d.id}_$timestamp.pdf")
    }

    fun exportLabReportCsv(context: Context, d: LabReportData) {
        val sb = StringBuilder()
        sb.appendLine("IDSR Lab Report (Final Result)")
        sb.appendLine("Generated,${SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())}")
        sb.appendLine()
        sb.appendLine("Field,Value")
        sb.appendLine("Lab Name,${d.lab_name ?: ""}")
        sb.appendLine("Date Lab Received,${DateUtils.formatIsoDate(d.date_lab_received)}")
        sb.appendLine("Specimen Condition,${d.specimen_condition ?: ""}")
        sb.appendLine("Test Types Performed,${d.test_types_performed ?: ""}")
        sb.appendLine("Final Lab Result,${d.final_lab_result ?: ""}")
        sb.appendLine("Date Sent to District,${DateUtils.formatIsoDate(d.date_lab_sent_district)}")
        sb.appendLine("Date District Received,${DateUtils.formatIsoDate(d.date_district_received_lab_result)}")
        sb.appendLine("Region,${d.region_name ?: ""}")
        sb.appendLine("District,${d.district_name ?: ""}")
        sb.appendLine("Submitted By,${d.full_name ?: ""}")
        sb.appendLine("Submitted At,${DateUtils.formatIsoDateTime(d.created_at)}")
        val imageCount = d.lab_result_images?.size ?: 0
        sb.appendLine("Lab Result Images,$imageCount image(s) attached — view in app")

        saveAndShare(context, sb.toString(), "labreport_${d.id}_$timestamp.csv", "text/csv")
    }
}