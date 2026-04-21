package com.jnetai.workcalc.util

import android.content.Context
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.property.TextAlignment
import com.itextpdf.layout.property.UnitValue
import com.jnetai.workcalc.data.entity.Employer
import com.jnetai.workcalc.data.entity.Shift
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object PayslipGenerator {

    data class PayslipData(
        val employer: Employer,
        val period: String,
        val shifts: List<Shift>,
        val totalRegularHours: Double,
        val totalOvertimeHours: Double,
        val grossPay: Double,
        val incomeTax: Double,
        val nationalInsurance: Double,
        val netPay: Double,
        val ucDeduction: Double,
        val takeHomePay: Double
    )

    fun generatePdf(context: Context, data: PayslipData): File {
        val dir = File(context.cacheDir, "payslips").apply { mkdirs() }
        val file = File(dir, "payslip_${data.employer.name}_${data.period.replace(" ", "_")}.pdf")

        val pdfDoc = PdfDocument(PdfWriter(file))
        val doc = Document(pdfDoc)

        // Title
        doc.add(Paragraph("Work Calc Pro - Payslip")
            .setFontSize(20f)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER))

        doc.add(Paragraph(""))

        // Employer info
        doc.add(Paragraph("Employer: ${data.employer.name}").setFontSize(14f).setBold())
        doc.add(Paragraph("Period: ${data.period}"))
        doc.add(Paragraph(""))

        // Shift details table
        val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 25f, 25f, 15f, 15f)))
            .setWidth(UnitValue.createPercentValue(100f))

        table.addHeaderCell(Cell().add(Paragraph("Date").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Time").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Break").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("Reg Hrs").setBold()))
        table.addHeaderCell(Cell().add(Paragraph("OT Hrs").setBold()))

        val sdf = SimpleDateFormat("EEE dd MMM", Locale.UK)
        for (shift in data.shifts) {
            val date = sdf.format(DateUtils.parseDate(shift.date))
            table.addCell(Cell().add(Paragraph(date)))
            table.addCell(Cell().add(Paragraph("${shift.startTime} - ${shift.endTime}")))
            table.addCell(Cell().add(Paragraph("${shift.breakMinutes}m")))
            table.addCell(Cell().add(Paragraph("%.1f".format(shift.getRegularHours(data.employer.overtimeThresholdHours)))))
            table.addCell(Cell().add(Paragraph("%.1f".format(shift.getOvertimeHours(data.employer.overtimeThresholdHours)))))
        }

        doc.add(table)
        doc.add(Paragraph(""))

        // Summary
        doc.add(Paragraph("Summary").setFontSize(14f).setBold())
        doc.add(Paragraph("Regular Hours: ${"%.1f".format(data.totalRegularHours)}"))
        doc.add(Paragraph("Overtime Hours: ${"%.1f".format(data.totalOvertimeHours)}"))
        doc.add(Paragraph("Gross Pay: ${UKCalculator.formatCurrency(data.grossPay)}"))
        doc.add(Paragraph("Income Tax: -${UKCalculator.formatCurrency(data.incomeTax)}"))
        doc.add(Paragraph("National Insurance: -${UKCalculator.formatCurrency(data.nationalInsurance)}"))
        doc.add(Paragraph("Net Pay: ${UKCalculator.formatCurrency(data.netPay)}"))
        if (data.ucDeduction > 0) {
            doc.add(Paragraph("UC Deduction: -${UKCalculator.formatCurrency(data.ucDeduction)}"))
        }
        doc.add(Paragraph(""))
        doc.add(Paragraph("Take Home Pay: ${UKCalculator.formatCurrency(data.takeHomePay)}")
            .setFontSize(16f)
            .setBold())

        doc.close()
        return file
    }

    fun generateText(data: PayslipData): String {
        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════")
        sb.appendLine("  Work Calc Pro - Payslip")
        sb.appendLine("═══════════════════════════════")
        sb.appendLine()
        sb.appendLine("Employer: ${data.employer.name}")
        sb.appendLine("Period: ${data.period}")
        sb.appendLine()
        sb.appendLine("─".repeat(40))

        val sdf = SimpleDateFormat("EEE dd MMM", Locale.UK)
        for (shift in data.shifts) {
            val date = sdf.format(DateUtils.parseDate(shift.date))
            sb.appendLine("$date | ${shift.startTime}-${shift.endTime} | Break: ${shift.breakMinutes}m")
        }

        sb.appendLine("─".repeat(40))
        sb.appendLine()
        sb.appendLine("Regular Hours:   ${"%.1f".format(data.totalRegularHours)}")
        sb.appendLine("Overtime Hours:  ${"%.1f".format(data.totalOvertimeHours)}")
        sb.appendLine("Gross Pay:       ${UKCalculator.formatCurrency(data.grossPay)}")
        sb.appendLine("Income Tax:     -${UKCalculator.formatCurrency(data.incomeTax)}")
        sb.appendLine("Nat. Insurance: -${UKCalculator.formatCurrency(data.nationalInsurance)}")
        sb.appendLine("Net Pay:         ${UKCalculator.formatCurrency(data.netPay)}")
        if (data.ucDeduction > 0) {
            sb.appendLine("UC Deduction:   -${UKCalculator.formatCurrency(data.ucDeduction)}")
        }
        sb.appendLine()
        sb.appendLine("Take Home:       ${UKCalculator.formatCurrency(data.takeHomePay)}")
        sb.appendLine("═══════════════════════════════")

        return sb.toString()
    }
}