package com.ducatus.data

import com.itextpdf.text.pdf.PdfPTable

data class TransactionReportTable(
    val expenseTable: PdfPTable,
    val incomeTable: PdfPTable,
    val expenseCount: Int,
    val incomeCount: Int
)
