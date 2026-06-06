package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntry
import com.example.data.HistoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = HistoryRepository(database.historyDao())

    // UI state
    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result.asStateFlow()

    private val _isCalculated = MutableStateFlow(false)

    // History Flow from Room
    val historyList: StateFlow<List<HistoryEntry>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onAction(action: CalculatorAction) {
        when (action) {
            is CalculatorAction.Digit -> handleDigit(action.digit)
            CalculatorAction.Decimal -> handleDecimal()
            is CalculatorAction.Operator -> handleOperator(action.operator)
            CalculatorAction.Clear -> handleClear()
            CalculatorAction.Backspace -> handleBackspace()
            CalculatorAction.ToggleSign -> handleToggleSign()
            CalculatorAction.Parentheses -> handleParentheses()
            CalculatorAction.Percent -> handlePercent()
            CalculatorAction.Calculate -> handleCalculate()
            is CalculatorAction.DeleteHistoryItem -> deleteHistoryItem(action.id)
            CalculatorAction.ClearHistory -> clearHistory()
            is CalculatorAction.SetExpression -> {
                _expression.value = action.exp
                _result.value = action.res
                _isCalculated.value = true
            }
        }
    }

    private fun handleDigit(digit: Int) {
        if (_isCalculated.value) {
            _expression.value = digit.toString()
            _isCalculated.value = false
            _result.value = ""
        } else {
            _expression.value += digit
        }
        autoEvaluate()
    }

    private fun handleDecimal() {
        if (_isCalculated.value) {
            _expression.value = "0."
            _isCalculated.value = false
            _result.value = ""
            return
        }

        val currentExp = _expression.value
        if (currentExp.isEmpty() || currentExp.endsWith(" ") || currentExp.endsWith("(")) {
            _expression.value += "0."
            return
        }

        // Find the start of the current number to see if it already has a decimal
        val lastNumber = currentExp.split("[+\\-×÷()]".toRegex()).lastOrNull() ?: ""
        if (!lastNumber.contains(".")) {
            _expression.value += "."
        }
    }

    private fun handleOperator(operatorStr: String) {
        _isCalculated.value = false
        val currentExp = _expression.value
        if (currentExp.isEmpty()) {
            if (operatorStr == "-") {
                _expression.value = "-"
            }
            return
        }

        val trimmed = currentExp.trim()
        val lastChar = trimmed.lastOrNull()

        if (lastChar != null && isOperatorChar(lastChar)) {
            // Replace previous operator
            _expression.value = currentExp.dropLast(2) + "$operatorStr "
        } else {
            _expression.value += " $operatorStr "
        }
    }

    private fun isOperatorChar(char: Char): Boolean {
        return char == '+' || char == '-' || char == '×' || char == '÷' || char == '*' || char == '/'
    }

    private fun handleClear() {
        _expression.value = ""
        _result.value = ""
        _isCalculated.value = false
    }

    private fun handleBackspace() {
        if (_isCalculated.value) {
            _isCalculated.value = false
        }
        val currentExp = _expression.value
        if (currentExp.isNotEmpty()) {
            if (currentExp.endsWith(" ")) {
                _expression.value = currentExp.dropLast(3) // drops operator and spaces (" + ")
            } else {
                _expression.value = currentExp.dropLast(1)
            }
        }
        autoEvaluate()
    }

    private fun handleToggleSign() {
        val currentExp = _expression.value
        if (currentExp.isEmpty()) {
            _expression.value = "(-"
            return
        }

        if (currentExp.endsWith("(-")) {
            _expression.value = currentExp.dropLast(2)
            autoEvaluate()
            return
        }

        // Check if there is an active expression
        if (currentExp.endsWith(" ")) {
            _expression.value += "(-"
        } else {
            // Find the last number and wrap it or toggle the prefix
            val lastNumberStart = currentExp.lastIndexOfAny(charArrayOf(' ', '('))
            if (lastNumberStart == -1) {
                _expression.value = "(-$currentExp"
            } else {
                val prefix = currentExp.substring(0, lastNumberStart + 1)
                val suffix = currentExp.substring(lastNumberStart + 1)
                if (suffix.startsWith("(-")) {
                    _expression.value = prefix + suffix.substring(2)
                } else {
                    _expression.value = "$prefix(-$suffix"
                }
            }
        }
        autoEvaluate()
    }

    private fun handleParentheses() {
        if (_isCalculated.value) {
            _expression.value = "("
            _isCalculated.value = false
            _result.value = ""
            return
        }

        val currentExp = _expression.value
        if (currentExp.isEmpty()) {
            _expression.value = "("
            return
        }

        val lastChar = currentExp.last()
        val openCount = currentExp.count { it == '(' }
        val closeCount = currentExp.count { it == ')' }

        if (openCount > closeCount && (lastChar.isDigit() || lastChar == ')' || lastChar == '%')) {
            _expression.value += ")"
        } else {
            if (lastChar.isDigit() || lastChar == ')' || lastChar == '%') {
                _expression.value += " × ("
            } else {
                _expression.value += "("
            }
        }
        autoEvaluate()
    }

    private fun handlePercent() {
        if (_isCalculated.value) {
            _isCalculated.value = false
        }
        val currentExp = _expression.value
        if (currentExp.isNotEmpty() && (currentExp.last().isDigit() || currentExp.last() == ')')) {
            _expression.value += "%"
        }
        autoEvaluate()
    }

    private fun handleCalculate() {
        val currentExp = _expression.value
        if (currentExp.trim().isEmpty()) return

        try {
            val rawResult = evaluate(currentExp)
            val formattedResult = formatResult(rawResult)
            _result.value = formattedResult

            // Save to database of History
            viewModelScope.launch {
                repository.insert(
                    HistoryEntry(
                        expression = currentExp,
                        result = formattedResult
                    )
                )
            }
            _isCalculated.value = true
        } catch (e: Exception) {
            _result.value = "Error"
        }
    }

    private fun autoEvaluate() {
        val currentExp = _expression.value.trim()
        if (currentExp.isEmpty()) {
            _result.value = ""
            return
        }
        // If it looks like a single digit or single decimal number, don't auto-calculate
        if (currentExp.matches("[0-9.-]+".toRegex())) {
            _result.value = ""
            return
        }
        try {
            val rawResult = evaluate(currentExp)
            _result.value = formatResult(rawResult)
        } catch (e: Exception) {
            // Quiet fail for auto-evaluation since typing is not complete yet
            _result.value = ""
        }
    }

    private fun evaluate(str: String): Double {
        // Safe evaluation using recursive descent parser
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < str.length) str[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                // Auto-close missing parenthesis at the end
                while (pos < str.length && eat(')'.code)) {
                    // consume trailing parenthesis
                }
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm()
                    else if (eat('-'.code)) x -= parseTerm()
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code) || eat('x'.code) || eat('×'.code)) {
                        x *= parseFactor()
                    } else if (eat('/'.code) || eat('÷'.code)) {
                        val divisor = parseFactor()
                        if (divisor == 0.0) throw ArithmeticException("Division by zero")
                        x /= divisor
                    } else {
                        return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    val valStr = str.substring(startPos, pos)
                    if (valStr == ".") {
                        x = 0.0
                    } else {
                        x = valStr.toDouble()
                    }
                } else {
                    x = 0.0
                }

                if (eat('%'.code)) {
                    x *= 0.01
                }
                return x
            }
        }.parse()
    }

    private fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        return try {
            val bd = BigDecimal(value).setScale(10, RoundingMode.HALF_UP)
            val stripZero = bd.stripTrailingZeros()
            // If the value is tiny or zero, make sure stripTrailingZeros doesn't result in plain string scientific notation
            if (stripZero.scale() <= 0) {
                stripZero.toBigInteger().toString()
            } else {
                stripZero.toPlainString()
            }
        } catch (e: Exception) {
            val longVal = value.toLong()
            if (value == longVal.toDouble()) {
                longVal.toString()
            } else {
                value.toString()
            }
        }
    }

    private fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    private fun clearHistory() {
        viewModelScope.launch {
            repository.clear()
        }
    }
}

sealed class CalculatorAction {
    data class Digit(val digit: Int) : CalculatorAction()
    object Decimal : CalculatorAction()
    data class Operator(val operator: String) : CalculatorAction()
    object Clear : CalculatorAction()
    object Backspace : CalculatorAction()
    object ToggleSign : CalculatorAction()
    object Parentheses : CalculatorAction()
    object Percent : CalculatorAction()
    object Calculate : CalculatorAction()
    data class DeleteHistoryItem(val id: Long) : CalculatorAction()
    object ClearHistory : CalculatorAction()
    data class SetExpression(val exp: String, val res: String) : CalculatorAction()
}
