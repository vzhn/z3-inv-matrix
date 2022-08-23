package me.vzhilin.matrix

import com.microsoft.z3.ArithExpr
import com.microsoft.z3.BoolExpr
import com.microsoft.z3.Context
import com.microsoft.z3.IntSort
import com.microsoft.z3.Status
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.File
import java.util.Scanner

enum class Matrix { A, B }
sealed class Formula {
    data class Value(val m: Matrix, val col: Int, val row: Int): Formula() {
        override fun toString() = "${m}[${col}][${row}]"
        val name get() = toString()
    }
    data class Sum(val vs: List<Formula>): Formula() {
        override fun toString() = vs.joinToString(" + ")
    }
    data class Prod(val left: Formula, val right: Formula): Formula() {
        override fun toString() = "$left * $right"
    }
    data class Eq(val left: Formula, val right: Formula): Formula() {
        override fun toString() = "$left = $right"
    }
    object Zero: Formula() {
        override fun toString() = "0"
    }
    object One: Formula() {
        override fun toString() = "1"
    }
}

class Main(cols: Int, mod: Int) {
    companion object {
        @JvmStatic fun main(argv: Array<String>) {
            val parser = ArgParser("smt-inv-matrix")
            val mod by parser.option(ArgType.Int, fullName = "mod").required()
            val cols by parser.option(ArgType.Int, fullName = "cols").required()
            val file by parser.option(ArgType.String, fullName = "file").required()
            parser.parse(argv)

            val matrix = readMatrix(file, cols)

            val instance = Main(cols, mod)
            val formulas = instance.multiplicationFormulas() + instance.matrixFormulas(matrix)
            val solver = instance.context.mkSolver()
            solver.add(*formulas.map(instance::conv).toTypedArray())
            val success = solver.check()
            if (success == Status.SATISFIABLE) {
                println(solver.model)
            }
        }
    }

    private val context = Context()

    // A * B = E
    private fun multiplicationFormulas(): List<Formula> {
        val n = 3
        val formulas = mutableListOf<Formula>()

        for (r in 0 until n) {
            for (c in 0 until n) {
                val expected = if (r == c) {
                    Formula.One
                } else {
                    Formula.Zero
                }

                val sum = Formula.Sum((0 until n).map { k ->
                    val a = Formula.Value(Matrix.A, k, r)
                    val b = Formula.Value(Matrix.B, c, k)
                    Formula.Prod(a, b)
                })

                formulas.add(Formula.Eq(sum, expected))
            }
        }
        return formulas
    }

    fun convInt(f: Formula): ArithExpr<IntSort> {
        return when (f) {
            is Formula.Eq -> throw IllegalArgumentException()
            is Formula.Prod -> {
                context.mkMul(convInt(f.left), convInt(f.right))
            }
            is Formula.Sum -> {
                context.mkAdd(*f.vs.map(::convInt).toTypedArray())
            }
            is Formula.Value -> {
                context.mkIntConst(f.name)
            }
            Formula.Zero -> context.mkInt(0)
            Formula.One -> context.mkInt(1)
        }
    }

    fun conv(f: Formula): BoolExpr {
        return when (f) {
            is Formula.Eq -> {
                context.mkEq(convInt(f.left), convInt(f.right))
            }
            else -> throw IllegalArgumentException()
        }
    }

    fun matrixFormulas(m: List<Int>): List<Formula> {
        TODO()
    }
}

fun readMatrix(fileName: String, cols: Int): List<Int> {
    val data = mutableListOf<Int>()
    val sc = Scanner(File(fileName))
    for (i in 0 until cols * cols) {
        data.add(sc.nextInt())
    }
    return data
}