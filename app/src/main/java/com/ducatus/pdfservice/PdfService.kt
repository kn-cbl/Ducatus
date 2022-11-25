package com.ducatus.pdfservice

import android.os.Build
import android.os.Environment
import com.ducatus.data.ExpenseReport
import com.ducatus.data.TransactionReportTable
import com.itextpdf.text.*
import com.itextpdf.text.pdf.PdfPCell
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class PdfService {
    private val titleFont = Font(Font.FontFamily.HELVETICA, 14f, Font.BOLD)
    private val titleFontNormal = Font(Font.FontFamily.HELVETICA, 14f, Font.NORMAL)
    private val bodyFontBold = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)
    private val bodyFont = Font(Font.FontFamily.HELVETICA, 12f, Font.NORMAL)
    private val footerFont = Font(Font.FontFamily.HELVETICA, 10f, Font.NORMAL)

    private fun createFile(): File {
        val fileName = "Expenses Report.pdf"
        val path =
            if (Build.VERSION.SDK_INT >= 30) Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            else Environment.getExternalStorageDirectory()

        val file = File(path, fileName)
        if (!file.exists()) file.createNewFile()
        return file
    }

    private fun createDocument(): Document {
        val document = Document()
        document.setMargins(24f, 24f, 32f, 32f)
        document.pageSize = PageSize.A4
        document.addCreationDate()
        return document
    }

    private fun createTable(column: Int, columnWidth: FloatArray): PdfPTable {
        val table = PdfPTable(column)
        table.widthPercentage = 100f
        table.setWidths(columnWidth)
        table.headerRows = 1
        table.defaultCell.verticalAlignment = Element.ALIGN_CENTER
        table.defaultCell.horizontalAlignment = Element.ALIGN_CENTER
        return table
    }

    private fun createCell(content: String, font: Font, horizontalAlignment: Int): PdfPCell {
        val cell = PdfPCell(Phrase(content, font))
        cell.horizontalAlignment = horizontalAlignment
        cell.verticalAlignment = Element.ALIGN_MIDDLE
        cell.setPadding(8f)
        cell.isUseAscender = true
        cell.paddingLeft = 4f
        cell.paddingRight = 4f
        cell.paddingTop = 8f
        cell.paddingBottom = 8f
        return cell
    }

    private fun addLineSpace(document: Document, number: Int) {
        document.add(Paragraph(""))
        for (i in 0 until number) {
            document.add(Paragraph(" "))
        }
    }

    private fun createParagraph(content: String, font: Font, alignment: Int): Paragraph{
        val paragraph = Paragraph(content, font)
        paragraph.alignment = alignment
        return paragraph
    }

    fun createPdf(
        name: String,
        date: String,
        expensesReport: MutableList<ExpenseReport>,
        onFinish: (file: File) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val file = createFile()
        val document = createDocument()

        val pdf = PdfWriter.getInstance(document, FileOutputStream(file))
        pdf.setFullCompression()
        document.open()

        // main title
        val title = createParagraph("Expenses Report for $name", titleFont, Element.ALIGN_CENTER)
        document.add(title)
        addLineSpace(document, 0)

        // date
        val dateParagraph = createParagraph(date, bodyFontBold, Element.ALIGN_CENTER)
        document.add(dateParagraph)
        addLineSpace(document, 2)

        // top five expenses
        val topFiveExpensesParagraph = createParagraph("Top five expenses", bodyFontBold, Element.ALIGN_LEFT)
        document.add(topFiveExpensesParagraph)
        addLineSpace(document, 1)

        val topFiveExpensesTable = createTopFiveExpensesTable(expensesReport)
        document.add(topFiveExpensesTable)
        addLineSpace(document, 2)

        // get expenses and income
        val transactionsTable = createTransactionsTable(expensesReport)

        // define expenses table
        val expenseParagraph = createParagraph("Total number of expenses: ${transactionsTable.expenseCount}", bodyFontBold, Element.ALIGN_LEFT)
        document.add(expenseParagraph)
        addLineSpace(document, 1)

        val expensesTable = transactionsTable.expenseTable
        document.add(expensesTable)
        addLineSpace(document, 2)

        // define income table
        val incomeParagraph = createParagraph("Total number of income: ${transactionsTable.incomeCount}", bodyFontBold, Element.ALIGN_LEFT)
        document.add(incomeParagraph)
        addLineSpace(document, 1)

        val incomeTable = transactionsTable.incomeTable
        document.add(incomeTable)
        addLineSpace(document, 1)

        // report generated date
        val zdtToday = ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault())
        val dtf = DateTimeFormatter.ofPattern("MMMM dd uuuu, h:mm a")
        val today = dtf.format(zdtToday)
        val reportGeneratedParagraph = createParagraph("Report Generated: $today", footerFont, Element.ALIGN_LEFT)
        document.add(reportGeneratedParagraph)
        addLineSpace(document, 1)

        document.close()
        onFinish(file)
    }

    private fun createTopFiveExpensesTable(expensesReport: MutableList<ExpenseReport>): PdfPTable {
        val dateWidth = 1.25f
        val nameWidth = 1.25f
        val categoryWidth = 1.25f
        val subcategoryWidth = 1.25f
        val paymentTypeWidth = 1.25f
        val amountWidth = 1.25f

        val columnWidth = floatArrayOf(dateWidth, nameWidth, categoryWidth, subcategoryWidth, paymentTypeWidth, amountWidth)
        val topFiveExpensesTable = createTable(6, columnWidth)
        val tableHeaders = listOf("Date", "Name", "Category", "Subcategory", "Payment Type", "Amount")
        tableHeaders.forEach {
            val cell = createCell(it, bodyFontBold, Element.ALIGN_CENTER)
            topFiveExpensesTable.addCell(cell)
        }

        // sort by amount
        expensesReport.sortByDescending { it.amount }
        val size =
            if (expensesReport.size < 5) expensesReport.size
            else 5

        var totalAmount = 0.0
        for (i in 0 until size) {
            totalAmount += expensesReport[i].amount

            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(expensesReport[i].date!!),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)

            val dateCell = createCell(formattedDate, bodyFont, Element.ALIGN_CENTER)
            topFiveExpensesTable.addCell(dateCell)

            val nameCell = createCell(expensesReport[i].name!!, bodyFont, Element.ALIGN_CENTER)
            topFiveExpensesTable.addCell(nameCell)

            val categoryCell = createCell(expensesReport[i].categoryName!!, bodyFont, Element.ALIGN_CENTER)
            topFiveExpensesTable.addCell(categoryCell)

            val subcategoryCell = createCell(expensesReport[i].subcategoryName ?: " ", bodyFont, Element.ALIGN_CENTER)
            topFiveExpensesTable.addCell(subcategoryCell)

            val paymentTypeCell = createCell(expensesReport[i].paymentType!!, bodyFont, Element.ALIGN_CENTER)
            topFiveExpensesTable.addCell(paymentTypeCell)

            val formattedAmount = "P" + String.format("%,.2f", expensesReport[i].amount)
            val expenseFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)

            // expense or income
            if (expensesReport[i].type == 0 || expensesReport[i].type == 2) {
                expenseFont.color = BaseColor.RED
                val amountCell = createCell(formattedAmount, expenseFont, Element.ALIGN_CENTER)
                topFiveExpensesTable.addCell(amountCell)
            }
        }

        val blankCell = createCell("", bodyFont, Element.ALIGN_CENTER)
        for (i in 0 until 4) {
            topFiveExpensesTable.addCell(blankCell)
        }

        val totalTextCell = createCell("Total", bodyFontBold, Element.ALIGN_RIGHT)
        topFiveExpensesTable.addCell(totalTextCell)

        val formattedExpense = "P" + String.format("%,.2f", totalAmount)

        val totalExpenseCell = createCell(formattedExpense, bodyFontBold, Element.ALIGN_CENTER)
        topFiveExpensesTable.addCell(totalExpenseCell)

        return topFiveExpensesTable
    }

    private fun createTransactionsTable(expensesReport: MutableList<ExpenseReport>): TransactionReportTable {
        val dateWidth = 1.25f
        val nameWidth = 1.25f
        val categoryWidth = 1.25f
        val subcategoryWidth = 1.25f
        val paymentTypeWidth = 1.25f
        val amountWidth = 1.25f

        val columnWidth = floatArrayOf(dateWidth, nameWidth, categoryWidth, subcategoryWidth, paymentTypeWidth, amountWidth)
        val expenseTable = createTable(6, columnWidth)
        val incomeTable = createTable(6, columnWidth)

        val tableHeaders = listOf("Date", "Name", "Category", "Subcategory", "Payment Type", "Amount")
        tableHeaders.forEach {
            val cell = createCell(it, bodyFontBold, Element.ALIGN_CENTER)
            expenseTable.addCell(cell)
            incomeTable.addCell(cell)
        }

        val totalAmount = mutableListOf(0.0, 0.0)
        val totalCount = mutableListOf(0, 0)

        expensesReport.sortByDescending { it.date }
        expensesReport.forEach { expenseReport ->
            val zdt = ZonedDateTime.ofInstant(
                Instant.ofEpochMilli(expenseReport.date!!),
                ZoneId.systemDefault()
            )
            val dtf = DateTimeFormatter.ofPattern("MMM dd, uuuu")
            val formattedDate = dtf.format(zdt)

            val dateCell = createCell(formattedDate, bodyFont, Element.ALIGN_CENTER)
            expenseTable.addCell(dateCell)

            val nameCell = createCell(expenseReport.name!!, bodyFont, Element.ALIGN_CENTER)
            expenseTable.addCell(nameCell)

            val categoryCell = createCell(expenseReport.categoryName!!, bodyFont, Element.ALIGN_CENTER)
            expenseTable.addCell(categoryCell)

            val subcategoryCell = createCell(expenseReport.subcategoryName ?: " ", bodyFont, Element.ALIGN_CENTER)
            expenseTable.addCell(subcategoryCell)

            val paymentTypeCell = createCell(expenseReport.paymentType!!, bodyFont, Element.ALIGN_CENTER)
            expenseTable.addCell(paymentTypeCell)

            val formattedAmount = "P" + String.format("%,.2f", expenseReport.amount)
            val expenseFont = Font(Font.FontFamily.HELVETICA, 12f, Font.BOLD)

            // expense or income
            if (expenseReport.type == 0 || expenseReport.type == 2) {
                totalAmount[0] += expenseReport.amount
                totalCount[0]++

                expenseFont.color = BaseColor.RED
                val amountCell = createCell(formattedAmount, expenseFont, Element.ALIGN_CENTER)
                expenseTable.addCell(amountCell)
            }
            else if (expenseReport.type == 1) {
                totalAmount[1] += expenseReport.amount
                totalCount[1]++

                expenseFont.color = BaseColor.GREEN
                val amountCell = createCell(formattedAmount, expenseFont, Element.ALIGN_CENTER)
                incomeTable.addCell(amountCell)
            }
        }

        val blankCell = createCell("", bodyFont, Element.ALIGN_CENTER)
        for (i in 0 until 4) {
            expenseTable.addCell(blankCell)
            incomeTable.addCell(blankCell)
        }

        val totalTextCell = createCell("Total", bodyFontBold, Element.ALIGN_RIGHT)
        expenseTable.addCell(totalTextCell)
        incomeTable.addCell(totalTextCell)

        val formattedExpense = "P" + String.format("%,.2f", totalAmount[0])
        val formattedIncome = "P" + String.format("%,.2f", totalAmount[1])

        val totalExpenseCell = createCell(formattedExpense, bodyFontBold, Element.ALIGN_CENTER)
        expenseTable.addCell(totalExpenseCell)

        val totalIncomeCell = createCell(formattedIncome, bodyFontBold, Element.ALIGN_CENTER)
        incomeTable.addCell(totalIncomeCell)

        return TransactionReportTable(
            expenseTable,
            incomeTable,
            totalCount[0],
            totalCount[1]
        )
    }
}