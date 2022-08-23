package me.vzhilin.matrix

import com.microsoft.z3.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required
import java.io.File
import java.util.Scanner

enum class Matrix { A, B }
sealed class Formula {
    data class Cell(val m: Matrix, val col: Int, val row: Int): Formula() {
        override fun toString() = "${m}[${col}][${row}]"
        val name get() = toString()
    }
    data class Sum(val vs: List<Formula>): Formula() {
        override fun toString() = vs.joinToString(" + ")
    }
    data class Prod(val left: Formula, val right: Formula): Formula() {
        override fun toString() = "$left * $right"
    }
    data class Mod(val lhs: Formula, val rhs: Formula): Formula()
    data class Eq(val left: Formula, val right: Formula): Formula() {
        override fun toString() = "$left = $right"
    }
    data class Ge(val left: Formula, val right: Formula): Formula() {
        override fun toString(): String {
            return "$left >= $right"
        }
    }
    data class Le(val left: Formula, val right: Formula): Formula() {
        override fun toString(): String {
            return "$left <= $right"
        }
    }
    data class Const(val n: Int): Formula() {
        override fun toString() = "$n"
    }
    object Zero: Formula() {
        override fun toString() = "0"
    }
    object One: Formula() {
        override fun toString() = "1"
    }
}

class Main(private val cols: Int, private val mod: Int) {
    companion object {
        @JvmStatic fun main(argv: Array<String>) {
            val parser = ArgParser("smt-inv-matrix")
            val mod by parser.option(ArgType.Int, fullName = "mod").required()
            val cols by parser.option(ArgType.Int, fullName = "cols").required()
            val file by parser.option(ArgType.String, fullName = "file").required()
            parser.parse(argv)

            val matrix = readMatrix(file, cols)

            val instance = Main(cols, mod)
            val formulas = instance.multiplicationFormulas() + instance.matrixFormulas(matrix) + instance.cellConstraints()
            val solver = instance.context.mkSolver("NIA")
            solver.add(*formulas.map(instance::conv).toTypedArray())
            val success = solver.check()
            if (success == Status.SATISFIABLE) {
                val result = instance.extractModel(solver.model)
                println("== input ==")
                instance.print(matrix)
                println()
                println("== result ==")
                instance.print(result)
                println()
                println("== input * result ==")
                instance.print(mul(cols, mod, matrix, result))
                println()
            } else {
                System.err.println("UNSAT")
            }
        }
    }

    private fun extractModel(model: Model): List<Int> {
        val rs = mutableListOf<Int>()
        for (rn in 0 until cols) {
            for (cn in 0 until cols) {
                val interp = model.getConstInterp(context.mkIntConst(Formula.Cell(Matrix.B, cn, rn).name)) as IntNum
                val value = interp.int
                rs.add(value)
            }
        }
        return rs
    }

    private fun print(matrix: List<Int>) {
        for (rn in 0 until cols) {
            for (cn in 0 until cols) {
                val value = matrix[cols * rn + cn]
                print("$value ")
            }
            println()
        }
    }

    private val context = Context()
    private val modConst = Formula.Const(mod)

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
                    val a = Formula.Cell(Matrix.A, k, r)
                    val b = Formula.Cell(Matrix.B, c, k)
                    Formula.Prod(a, b)
                })

                formulas.add(Formula.Eq(Formula.Mod(sum, modConst), expected))
            }
        }
        return formulas
    }

    private fun cellConstraints(): List<Formula> {
        return (0 until cols * cols).map { it ->
            val col = it % cols
            val row = it / cols
            col to row
        }.flatMap { (col, row) ->
            listOf(
                Formula.Ge(Formula.Cell(Matrix.B, col, row), Formula.Zero),
                Formula.Le(Formula.Cell(Matrix.B, col, row), Formula.Const(mod - 1))
            )
        }
    }

    private fun convInt(f: Formula): ArithExpr<IntSort> {
        return when (f) {
            is Formula.Prod -> context.mkMul(convInt(f.left), convInt(f.right))
            is Formula.Sum -> context.mkAdd(*f.vs.map(::convInt).toTypedArray())
            is Formula.Cell -> context.mkIntConst(f.name)
            is Formula.Const -> context.mkInt(f.n)
            is Formula.Mod -> context.mkMod(convInt(f.lhs), convInt(f.rhs))
            Formula.Zero -> context.mkInt(0)
            Formula.One -> context.mkInt(1)
            else -> throw IllegalArgumentException()
        }
    }

    fun conv(f: Formula): BoolExpr {
        return when (f) {
            is Formula.Eq -> context.mkEq(convInt(f.left), convInt(f.right))
            is Formula.Ge -> context.mkGe(convInt(f.left), convInt(f.right))
            is Formula.Le -> context.mkLe(convInt(f.left), convInt(f.right))
            else -> throw IllegalArgumentException()
        }
    }

    fun matrixFormulas(m: List<Int>): List<Formula> {
        val rs = mutableListOf<Formula>()
        for (i in 0 until cols * cols) {
            val col = i % cols
            val row = i / cols
            rs.add(Formula.Eq(Formula.Cell(Matrix.A, col, row), Formula.Const(m[i])))
        }
        return rs
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

fun mul(cols: Int, mod: Int, a: List<Int>, b: List<Int>): List<Int> {
    val rs = mutableListOf<Int>()
    for (row in 0 until cols) {
        for (col in 0 until cols) {
            var sum = 0
            for (k in 0 until cols) {
                sum += a[row * cols + k] * b[k * cols + col]
            }
            rs.add(sum % mod)
        }
    }
    return rs
}