package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

object ShareAndPrintUtils {

    /**
     * Automatically generates a filename starting with the Customer Name.
     */
    fun generateCustomerFileName(customerName: String?, documentType: String, extension: String = "pdf"): String {
        val cleanCustomer = (customerName?.trim()?.ifBlank { null } ?: "Customer")
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim()
        val cleanDoc = documentType.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .trim()
        val ext = extension.removePrefix(".").trim()
        return if (ext.isNotBlank()) {
            "${cleanCustomer}_$cleanDoc.$ext"
        } else {
            "${cleanCustomer}_$cleanDoc"
        }
    }

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "मोबाइल नंबर उपलब्ध नहीं है", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${phoneNumber.trim()}")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "कॉल करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareToWhatsApp(context: Context, phoneNumber: String, message: String, customerName: String? = null, documentType: String? = null) {
        val cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
        val formattedNumber = when {
            cleanNumber.length == 10 -> "91$cleanNumber"
            cleanNumber.startsWith("91") -> cleanNumber
            cleanNumber.isNotBlank() -> cleanNumber
            else -> ""
        }

        try {
            val encodedMsg = URLEncoder.encode(message, "UTF-8")
            val uri = if (formattedNumber.isNotBlank()) {
                Uri.parse("https://wa.me/$formattedNumber?text=$encodedMsg")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encodedMsg")
            }
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            val title = if (customerName != null && documentType != null) {
                generateCustomerFileName(customerName, documentType, "pdf")
            } else {
                "REMS Business"
            }
            sharePlainText(context, title, message, customerName, documentType)
        }
    }

    fun sharePlainText(
        context: Context,
        title: String,
        text: String,
        customerName: String? = null,
        documentType: String? = null
    ) {
        val subject = if (!customerName.isNullOrBlank() && !documentType.isNullOrBlank()) {
            generateCustomerFileName(customerName, documentType, "pdf")
        } else {
            title
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TITLE, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    /**
     * Generates a real PDF file with the automatic Customer Name filename pattern
     * and triggers the Android share sheet with the attached PDF document.
     */
    fun createAndSharePdf(
        context: Context,
        customerName: String?,
        documentType: String,
        contentText: String
    ): File? {
        val fileName = generateCustomerFileName(customerName, documentType, "pdf")
        val pdfFile = createPdfDocumentFile(context, fileName, contentText)
        if (pdfFile != null && pdfFile.exists()) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    pdfFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    putExtra(Intent.EXTRA_TITLE, fileName)
                    putExtra(Intent.EXTRA_TEXT, contentText)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, fileName))
                Toast.makeText(context, "PDF तैयार: $fileName", Toast.LENGTH_SHORT).show()
                return pdfFile
            } catch (e: Exception) {
                Toast.makeText(context, "PDF साझा करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
                sharePlainText(context, fileName, contentText, customerName, documentType)
            }
        } else {
            sharePlainText(context, fileName, contentText, customerName, documentType)
        }
        return null
    }

    /**
     * Creates an actual PDF file on disk with clean layout and typography.
     */
    fun createPdfDocumentFile(
        context: Context,
        fileName: String,
        contentText: String
    ): File? {
        return try {
            val file = File(context.cacheDir, fileName)
            val pdfDoc = PdfDocument()

            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points
            val margin = 40f
            val printableWidth = pageWidth - (margin * 2)

            val paint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                isAntiAlias = true
            }

            val headerPaint = Paint().apply {
                color = Color.rgb(10, 30, 60)
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subHeaderPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                isAntiAlias = true
            }

            val lineSpacing = 16f
            val lines = contentText.lines()
            val wrappedLines = mutableListOf<String>()

            for (line in lines) {
                if (line.isBlank()) {
                    wrappedLines.add("")
                    continue
                }
                // Word wrap long lines
                val words = line.split(" ")
                var currentLine = ""
                for (word in words) {
                    val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
                    val textWidth = paint.measureText(testLine)
                    if (textWidth <= printableWidth) {
                        currentLine = testLine
                    } else {
                        if (currentLine.isNotEmpty()) {
                            wrappedLines.add(currentLine)
                        }
                        currentLine = word
                    }
                }
                if (currentLine.isNotEmpty()) {
                    wrappedLines.add(currentLine)
                }
            }

            var currentLineIndex = 0
            var pageNumber = 1

            while (currentLineIndex < wrappedLines.size || pageNumber == 1) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Background
                canvas.drawColor(Color.WHITE)

                var yPos = margin + 20f

                // Header Banner on Page 1
                if (pageNumber == 1) {
                    canvas.drawText("RAJAT ELECTRO-MECH SOLUTIONS (REMS)", margin, yPos, headerPaint)
                    yPos += 16f
                    canvas.drawText("Uchana • Mobile: 8685058336 (Sushil Sheokand)", margin, yPos, subHeaderPaint)
                    yPos += 14f

                    // Divider line
                    val dividerPaint = Paint().apply {
                        color = Color.LTGRAY
                        strokeWidth = 1f
                    }
                    canvas.drawLine(margin, yPos, pageWidth - margin, yPos, dividerPaint)
                    yPos += 18f
                }

                while (currentLineIndex < wrappedLines.size && yPos < (pageHeight - margin)) {
                    val lineText = wrappedLines[currentLineIndex]
                    if (lineText.startsWith("*") || lineText.startsWith("🧾") || lineText.startsWith("🛡️") || lineText.startsWith("📒")) {
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    } else {
                        paint.typeface = Typeface.DEFAULT
                    }
                    canvas.drawText(lineText, margin, yPos, paint)
                    yPos += lineSpacing
                    currentLineIndex++
                }

                // Footer
                val footerPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 8.5f
                }
                canvas.drawText("Page $pageNumber • Generated by REMS Mobile App", margin, pageHeight - margin + 15f, footerPaint)

                pdfDoc.finishPage(page)
                pageNumber++

                if (currentLineIndex >= wrappedLines.size) break
            }

            val fos = FileOutputStream(file)
            pdfDoc.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDoc.close()

            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves and shares an image file with automatic customer-based filename.
     */
    fun createAndShareImage(
        context: Context,
        customerName: String?,
        documentType: String = "Image",
        bitmap: Bitmap
    ): File? {
        val fileName = generateCustomerFileName(customerName, documentType, "jpg")
        return try {
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, fileName)
                putExtra(Intent.EXTRA_TITLE, fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, fileName))
            Toast.makeText(context, "Image तैयार: $fileName", Toast.LENGTH_SHORT).show()
            file
        } catch (e: Exception) {
            Toast.makeText(context, "Image साझा करने में त्रुटि: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Prints or Saves to PDF using Android's Print Spooler.
     */
    fun printCustomerDocument(
        context: Context,
        customerName: String?,
        documentType: String,
        contentText: String
    ) {
        val fileName = generateCustomerFileName(customerName, documentType, "pdf")
        val jobName = generateCustomerFileName(customerName, documentType, "")

        val pdfFile = createPdfDocumentFile(context, fileName, contentText)
        if (pdfFile != null && pdfFile.exists()) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val printAdapter = object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(fileName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            val input = FileInputStream(pdfFile)
                            val output = FileOutputStream(destination?.fileDescriptor)
                            val buffer = ByteArray(1024)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } >= 0) {
                                if (cancellationSignal?.isCanceled == true) {
                                    callback?.onWriteCancelled()
                                    input.close()
                                    output.close()
                                    return
                                }
                                output.write(buffer, 0, bytesRead)
                            }
                            input.close()
                            output.flush()
                            output.close()
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                }
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                return
            }
        }
        // Fallback
        createAndSharePdf(context, customerName, documentType, contentText)
    }

    fun generateUniqueSerialCode(): String {
        val letters = "ABCDEFGHJKLMNPQRSTUVWXYZ"
        val digits = "23456789"
        val random = Random()
        val p1 = letters[random.nextInt(letters.length)]
        val p2 = digits[random.nextInt(digits.length)]
        val p3 = letters[random.nextInt(letters.length)]
        val p4 = digits[random.nextInt(digits.length)]
        return "$p1$p2$p3$p4"
    }

    fun getYearLetterCode(year: Int): String {
        val index = (year - 2001) % 26
        return ('A' + (if (index >= 0) index else 0)).toString()
    }

    fun getMonthLetterCode(month: Int): String {
        val m = if (month in 1..12) month - 1 else 0
        return ('A' + m).toString()
    }

    fun generateBillNumber(dateStr: String, sequenceNum: Int): String {
        val cal = Calendar.getInstance()
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.parse(dateStr)?.let { cal.time = it }
        } catch (_: Exception) {}

        val yCode = getYearLetterCode(cal.get(Calendar.YEAR))
        val mCode = getMonthLetterCode(cal.get(Calendar.MONTH) + 1)
        val seqStr = sequenceNum.toString().padStart(4, '0')
        return "$yCode$mCode$seqStr"
    }

    fun formatDateToIndian(dateStr: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getCurrentDateIndian(): String {
        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return outputFormat.format(Date())
    }

    fun getCurrentDateIso(): String {
        val outputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return outputFormat.format(Date())
    }
}
