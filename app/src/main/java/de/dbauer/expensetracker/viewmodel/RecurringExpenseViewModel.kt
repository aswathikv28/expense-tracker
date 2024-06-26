package de.dbauer.expensetracker.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.dbauer.expensetracker.data.Recurrence
import de.dbauer.expensetracker.data.RecurringExpenseData
import de.dbauer.expensetracker.toCurrencyString
import de.dbauer.expensetracker.ui.customizations.ExpenseColor
import de.dbauer.expensetracker.viewmodel.database.ExpenseRepository
import de.dbauer.expensetracker.viewmodel.database.RecurrenceDatabase
import de.dbauer.expensetracker.viewmodel.database.RecurringExpense
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RecurringExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
) : ViewModel() {
    private val _recurringExpenseData = mutableStateListOf<RecurringExpenseData>()
    val recurringExpenseData: ImmutableList<RecurringExpenseData>
        get() = _recurringExpenseData.toImmutableList()

    private var _weeklyExpense by mutableStateOf("")
    private var _monthlyExpense by mutableStateOf("")
    private var _yearlyExpense by mutableStateOf("")
    val weeklyExpense: String
        get() = _weeklyExpense
    val monthlyExpense: String
        get() = _monthlyExpense
    val yearlyExpense: String
        get() = _yearlyExpense

    init {
        viewModelScope.launch {
            expenseRepository.allRecurringExpensesByPrice.collect { recurringExpenses ->
                onDatabaseUpdated(recurringExpenses)
            }
        }
    }

    fun addRecurringExpense(recurringExpense: RecurringExpenseData) {
        viewModelScope.launch {
            expenseRepository.insert(
                RecurringExpense(
                    id = 0,
                    name = recurringExpense.name,
                    description = recurringExpense.description,
                    price = recurringExpense.price,
                    everyXRecurrence = recurringExpense.everyXRecurrence,
                    recurrence = getRecurrenceIntFromUIRecurrence(recurringExpense.recurrence),
                    firstPayment = recurringExpense.firstPayment,
                    color = recurringExpense.color.toInt(),
                ),
            )
        }
    }

    fun editRecurringExpense(recurringExpense: RecurringExpenseData) {
        viewModelScope.launch {
            expenseRepository.update(
                RecurringExpense(
                    id = recurringExpense.id,
                    name = recurringExpense.name,
                    description = recurringExpense.description,
                    price = recurringExpense.price,
                    everyXRecurrence = recurringExpense.everyXRecurrence,
                    recurrence = getRecurrenceIntFromUIRecurrence(recurringExpense.recurrence),
                    firstPayment = recurringExpense.firstPayment,
                    color = recurringExpense.color.toInt(),
                ),
            )
        }
    }

    fun deleteRecurringExpense(recurringExpense: RecurringExpenseData) {
        viewModelScope.launch {
            expenseRepository.delete(
                RecurringExpense(
                    id = recurringExpense.id,
                    name = recurringExpense.name,
                    description = recurringExpense.description,
                    price = recurringExpense.price,
                    everyXRecurrence = recurringExpense.everyXRecurrence,
                    recurrence = getRecurrenceIntFromUIRecurrence(recurringExpense.recurrence),
                    firstPayment = recurringExpense.firstPayment,
                    color = recurringExpense.color.toInt(),
                ),
            )
        }
    }

    fun onDatabaseRestored() {
        viewModelScope.launch {
            val recurringExpenses = expenseRepository.allRecurringExpensesByPrice.first()
            onDatabaseUpdated(recurringExpenses)
        }
    }

    private fun onDatabaseUpdated(recurringExpenses: List<RecurringExpense>) {
        _recurringExpenseData.clear()
        recurringExpenses.forEach {
            _recurringExpenseData.add(
                RecurringExpenseData(
                    id = it.id,
                    name = it.name!!,
                    description = it.description!!,
                    price = it.price!!,
                    monthlyPrice = it.getMonthlyPrice(),
                    everyXRecurrence = it.everyXRecurrence!!,
                    recurrence = getRecurrenceFromDatabaseInt(it.recurrence!!),
                    firstPayment = it.firstPayment!!,
                    color = ExpenseColor.fromInt(it.color),
                ),
            )
        }
        _recurringExpenseData.sortByDescending { it.monthlyPrice }
        updateExpenseSummary()
    }

    private fun getRecurrenceFromDatabaseInt(recurrenceInt: Int): Recurrence {
        return when (recurrenceInt) {
            RecurrenceDatabase.Daily.value -> Recurrence.Daily
            RecurrenceDatabase.Weekly.value -> Recurrence.Weekly
            RecurrenceDatabase.Monthly.value -> Recurrence.Monthly
            RecurrenceDatabase.Yearly.value -> Recurrence.Yearly
            else -> Recurrence.Monthly
        }
    }

    private fun getRecurrenceIntFromUIRecurrence(recurrence: Recurrence): Int {
        return when (recurrence) {
            Recurrence.Daily -> RecurrenceDatabase.Daily.value
            Recurrence.Weekly -> RecurrenceDatabase.Weekly.value
            Recurrence.Monthly -> RecurrenceDatabase.Monthly.value
            Recurrence.Yearly -> RecurrenceDatabase.Yearly.value
        }
    }

    private fun updateExpenseSummary() {
        var totalMonthlyExpense = 0f
        var totalYearlyExpense = 0f
        var totalWeeklyExpense = 0f

        _recurringExpenseData.forEach { recurringExpense ->
            val monthlyExpense: Float
            val yearlyExpense: Float
            val weeklyExpense: Float

            when (recurringExpense.recurrence) {
                Recurrence.Daily -> {
                    val dailyPrice = recurringExpense.price * recurringExpense.everyXRecurrence
                    weeklyExpense = dailyPrice * 7
                    monthlyExpense = dailyPrice * 30 // Assuming 30 days in a month
                    yearlyExpense = dailyPrice * 365 // Assuming 365 days in a year
                }
                Recurrence.Weekly -> {
                    weeklyExpense = recurringExpense.price * recurringExpense.everyXRecurrence
                    monthlyExpense = weeklyExpense * 4 // Assuming 4 weeks in a month
                    yearlyExpense = monthlyExpense * 12
                }
                Recurrence.Monthly -> {
                    monthlyExpense = recurringExpense.price * recurringExpense.everyXRecurrence
                    yearlyExpense = monthlyExpense * 12
                    weeklyExpense = monthlyExpense / 4 // Assuming 4 weeks in a month
                }
                Recurrence.Yearly -> {
                    yearlyExpense = recurringExpense.price * recurringExpense.everyXRecurrence
                    monthlyExpense = yearlyExpense / 12
                    weeklyExpense = monthlyExpense / 4 // Assuming 4 weeks in a month
                }
            }

            totalMonthlyExpense += monthlyExpense
            totalYearlyExpense += yearlyExpense
            totalWeeklyExpense += weeklyExpense
        }

        _weeklyExpense = totalWeeklyExpense.toCurrencyString()
        _monthlyExpense = totalMonthlyExpense.toCurrencyString()
        _yearlyExpense = totalYearlyExpense.toCurrencyString()
    }


    companion object {
        fun create(expenseRepository: ExpenseRepository): ViewModelProvider.Factory {
            return viewModelFactory {
                initializer {
                    RecurringExpenseViewModel(expenseRepository)
                }
            }
        }
    }
}
