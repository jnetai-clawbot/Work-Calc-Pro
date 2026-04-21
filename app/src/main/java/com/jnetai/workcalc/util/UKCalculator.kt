package com.jnetai.workcalc.util

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * UK-specific tax, National Insurance, and Universal Credit calculator.
 * Rates for 2024/25 tax year.
 */
object UKCalculator {

    // --- Tax Year 2024/25 Constants ---
    const val PERSONAL_ALLOWANCE = 12570.0
    const val BASIC_RATE_LIMIT = 50270.0      // Income above this taxed at higher rate
    const val BASIC_RATE = 0.20
    const val HIGHER_RATE = 0.40
    const val ADDITIONAL_RATE = 0.45
    const val ADDITIONAL_RATE_THRESHOLD = 125140.0

    // Personal allowance taper
    const val PA_TAPER_THRESHOLD = 100000.0  // PA reduces by £1 for every £2 above
    const val PA_TAPER_RATE = 0.50

    // National Insurance Class 1 (2024/25)
    const val NI_PRIMARY_THRESHOLD_WEEKLY = 242.0
    const val NI_UPPER_LIMIT_WEEKLY = 967.0
    const val NI_MAIN_RATE = 0.08           // 8% between PT and UEL
    const val NI_UPPER_RATE = 0.02          // 2% above UEL

    // Universal Credit
    const val UC_TAPER_RATE = 0.55          // 55% reduction rate
    const val UC_WORK_ALLOWANCE_STANDARD = 379.0   // Monthly work allowance (no housing)
    const val UC_WORK_ALLOWANCE_HOUSING = 631.0     // Monthly work allowance (with housing)

    data class TaxBreakdown(
        val grossPay: Double,
        val personalAllowanceUsed: Double,
        val taxableIncome: Double,
        val incomeTax: Double,
        val nationalInsurance: Double,
        val netPay: Double,
        val effectiveTaxRate: Double
    )

    data class UCDeduction(
        val grossMonthlyEarnings: Double,
        val workAllowance: Double,
        val earningsAfterAllowance: Double,
        val ucDeduction: Double,
        val netAfterUC: Double
    )

    data class FullBreakdown(
        val grossPay: Double,
        val incomeTax: Double,
        val nationalInsurance: Double,
        val netPay: Double,
        val ucDeduction: Double,
        val finalTakeHome: Double,
        val effectiveRate: Double
    )

    /**
     * Calculate annual income tax from annual gross income.
     */
    fun calculateAnnualIncomeTax(annualGross: Double): Double {
        val effectivePA = calculateEffectivePersonalAllowance(annualGross)
        val taxable = maxOf(0.0, annualGross - effectivePA)

        var tax = 0.0
        // Basic rate band
        val basicBand = BASIC_RATE_LIMIT - PERSONAL_ALLOWANCE
        if (taxable <= basicBand) {
            tax = taxable * BASIC_RATE
        } else {
            tax = basicBand * BASIC_RATE
            // Higher rate band
            val higherBand = ADDITIONAL_RATE_THRESHOLD - BASIC_RATE_LIMIT
            val remaining = taxable - basicBand
            if (remaining <= higherBand) {
                tax += remaining * HIGHER_RATE
            } else {
                tax += higherBand * HIGHER_RATE
                // Additional rate
                tax += (remaining - higherBand) * ADDITIONAL_RATE
            }
        }
        return round(tax)
    }

    /**
     * Personal allowance tapers away above £100k.
     */
    fun calculateEffectivePersonalAllowance(annualGross: Double): Double {
        if (annualGross <= PA_TAPER_THRESHOLD) return PERSONAL_ALLOWANCE
        val reduction = (annualGross - PA_TAPER_THRESHOLD) * PA_TAPER_RATE
        return maxOf(0.0, PERSONAL_ALLOWANCE - reduction)
    }

    /**
     * Calculate annual National Insurance from annual gross income.
     */
    fun calculateAnnualNI(annualGross: Double): Double {
        val weeklyGross = annualGross / 52.0
        var ni = 0.0

        if (weeklyGross > NI_PRIMARY_THRESHOLD_WEEKLY) {
            val upperBand = minOf(weeklyGross, NI_UPPER_LIMIT_WEEKLY) - NI_PRIMARY_THRESHOLD_WEEKLY
            ni = upperBand * NI_MAIN_RATE

            if (weeklyGross > NI_UPPER_LIMIT_WEEKLY) {
                ni += (weeklyGross - NI_UPPER_LIMIT_WEEKLY) * NI_UPPER_RATE
            }
        }

        return round(ni * 52.0)
    }

    /**
     * Full tax breakdown for a given gross pay (annual).
     */
    fun fullBreakdown(annualGross: Double): TaxBreakdown {
        val tax = calculateAnnualIncomeTax(annualGross)
        val ni = calculateAnnualNI(annualGross)
        val net = annualGross - tax - ni
        val effectivePA = calculateEffectivePersonalAllowance(annualGross)
        val taxable = maxOf(0.0, annualGross - effectivePA)
        val effectiveRate = if (annualGross > 0) (tax + ni) / annualGross else 0.0

        return TaxBreakdown(
            grossPay = round(annualGross),
            personalAllowanceUsed = round(effectivePA),
            taxableIncome = round(taxable),
            incomeTax = round(tax),
            nationalInsurance = round(ni),
            netPay = round(net),
            effectiveTaxRate = round(effectiveRate * 100) / 100
        )
    }

    /**
     * UC deduction from monthly gross earnings.
     */
    fun calculateUCDeduction(
        monthlyGrossEarnings: Double,
        hasHousingCosts: Boolean = false,
        monthlyUCStandardAllowance: Double = 0.0
    ): UCDeduction {
        val workAllowance = if (hasHousingCosts) UC_WORK_ALLOWANCE_HOUSING else UC_WORK_ALLOWANCE_STANDARD
        val earningsAfterAllowance = maxOf(0.0, monthlyGrossEarnings - workAllowance)
        val deduction = round(earningsAfterAllowance * UC_TAPER_RATE)
        val netAfterUC = monthlyGrossEarnings - deduction

        return UCDeduction(
            grossMonthlyEarnings = round(monthlyGrossEarnings),
            workAllowance = round(workAllowance),
            earningsAfterAllowance = round(earningsAfterAllowance),
            ucDeduction = deduction,
            netAfterUC = round(netAfterUC)
        )
    }

    /**
     * Complete breakdown: tax + NI + UC for monthly gross.
     */
    fun completeMonthlyBreakdown(
        monthlyGross: Double,
        hasHousingCosts: Boolean = false
    ): FullBreakdown {
        val annualGross = monthlyGross * 12.0
        val annualTax = calculateAnnualIncomeTax(annualGross)
        val annualNI = calculateAnnualNI(annualGross)
        val monthlyTax = round(annualTax / 12.0)
        val monthlyNI = round(annualNI / 12.0)
        val netPay = round(monthlyGross - monthlyTax - monthlyNI)
        val uc = calculateUCDeduction(monthlyGross, hasHousingCosts)
        val finalTakeHome = round(netPay - uc.ucDeduction)
        val effectiveRate = if (monthlyGross > 0)
            round((monthlyTax + monthlyNI + uc.ucDeduction) / monthlyGross * 100) / 100
        else 0.0

        return FullBreakdown(
            grossPay = round(monthlyGross),
            incomeTax = monthlyTax,
            nationalInsurance = monthlyNI,
            netPay = netPay,
            ucDeduction = uc.ucDeduction,
            finalTakeHome = finalTakeHome,
            effectiveRate = effectiveRate
        )
    }

    private fun round(value: Double): Double {
        return BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
    }

    fun formatCurrency(value: Double): String {
        return "£%,.2f".format(value)
    }
}