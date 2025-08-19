import java.nio.file.Path
import kotlin.io.path.readText

private val SSymbol.asKeySort: String
    get() = when(text) {
        "Int" -> "int"
        "Bool" -> "bool"
        else -> text
    }

private val SExpr.text
    get() = (this as SSymbol).symbol.text

private fun SList.firstSymbol() =
    (this.seq.firstOrNull() as SSymbol).symbol.text

private fun SList.secondSymbol() =
    (this.seq[1] as SSymbol).symbol.text

private fun SList.tail() = subList(1, size)

class Translator {
    fun smt2key(filename: Path): List<String> {
        val content = filename.readText()
        return smt2key(content)
    }

    fun smt2key(content: String): List<String> {
        val smt = SMTParser(content).parse()
        smt.forEach { processToplevel(it) }
        return goals.map { output(it) }
    }

    private val stack = mutableListOf<MutableList<SList>>(mutableListOf())
    private fun pop() = stack.removeLast()
    private fun push() = stack.add(ArrayList())
    private fun assert(x: SList) = stack.last().add(x)

    val goals = mutableListOf<List<SList>>()
    private fun createGoal() {
        goals.add(stack.flatten().toList())
    }

    fun processToplevel(expr: SExpr) {
        require(expr is SList) { expr.text }
        when (expr.firstSymbol()) {
            "check-sat" -> createGoal()
            "pop" -> pop()
            "push" -> push()
            else -> assert(expr)
        }
    }

    fun output(x: List<SList>) = buildString {
        append("// KeY File generated of filename")

        appendSorts(x)
        appendFunctions(x)
        appendPredicates(x)
        appendRules(x)
        appendProblem(x)
    }

    private fun StringBuilder.appendSorts(declares: List<SList>) {
        append("\n\n\\sorts {\n")
        declares.filter { it.firstSymbol() == "declare-sort" }
            .forEach {
                append("${it.secondSymbol()};")
            }
        append("}")
    }

    private fun StringBuilder.appendFunctions(x: List<SList>) {
        append("\n\n\\functions {\n")
        for (it in x) {
            if (it.size != 4) continue
            val (func, name, args, ret) = it.seq
            if (func.text != "declare-fun" || func.text != "define-fun") continue
            if (ret.text == "Bool") continue

            val kargs = (args as SList).seq.joinToString(",") { it.text }
            append("\t${ret.text} ${name.text}($kargs);\n")
        }
        append("}")
    }

    private fun StringBuilder.appendPredicates(x: List<SList>) {
        append("\n\n\\predicates {\n")
        for (it in x) {
            if (it.size < 4) continue
            val (func, name, args, ret) = it.seq

            if (func.text != "declare-fun" || func.text != "define-fun") continue
            if (ret.text != "Bool") continue

            val kargs = (args as SList).seq.joinToString(",") { it.text }
            append("\t${name.text}($kargs);\n")
        }
        append("}")
    }

    private fun StringBuilder.appendRules(x: List<SList>) {
        append("\n\n\\rules {\n")
        for (it in x) {
            if (it.size != 5) continue
            val (func, name, args, _, rhs) = it.seq
            if (func.text == "define-fun") {
                append("\tdef_${name.text} {\n")
                (args as SList).seq.forEach {
                    val sort = (it as SList)[1] as SSymbol
                    val name = it[0] as SSymbol
                    append("\t\t\\schemaVar \\term ${sort.asKeySort} ${name.text};\n")
                }
                val argNames = args.filterIsInstance<SList>().joinToString(",") { it[0].text }

                append("\t\t\\find( ${name.text}($argNames) )\n")
                append("\t\t\\replacewith( ${asExpr(rhs)} )\n")
                append("\t};\n")
            }
        }
        append("}")
    }

    private fun StringBuilder.appendProblem(commands: List<SList>) {
        val formulae = commands
            .filter { it.firstSymbol() == "assert" }
            .joinToString(",\n\t") {
                val (_, expr) = it.seq
                asExpr(expr)
            }
        append("\n\n\\problem {\n\t$formulae\n\n\t==>\n}")
    }

    fun asExpr(x: SExpr): String =
        when (x) {
            is SSymbol -> x.text
            is SList ->
                when (x.firstSymbol()) {
                    "and" -> expandTerm("&", x)
                    "or" -> expandTerm("|", x)
                    "=>" -> expandTerm("->", x)
                    "+" -> expandTerm("+", x)
                    "-" -> expandTerm("-", x)
                    "*" -> expandTerm("*", x)
                    "/" -> expandTerm("/", x)
                    "mod" -> expandTerm("%", x)

                    // needs more care for more than 2 args
                    "<" -> expandTerm("<", x)
                    "<=" -> expandTerm("<=", x)
                    ">" -> expandTerm(">", x)
                    ">=" -> expandTerm(">=", x)
                    "=" -> expandTerm("=", x)

                    "ite" -> ifThenElse(x)
                    "let" -> letExpr(x)
                    else -> function(x)
                }
        }

    fun function(x: SList): String {
        val args = x.tail().joinToString(", ") { asExpr(it) }
        return "${x.firstSymbol()}($args)"
    }

    fun letExpr(x: SList) = buildString {
        val exprs = x[1] as SList
        for (it in exprs) {
            val (name, term) = it as SList
            append("\\{$name:=${asExpr(term)}}\\}")
        }
        append(asExpr(x[2]))
    }

    fun ifThenElse(x: SList) =
        "\\if (${asExpr(x[1])}); (${asExpr(x[2])})\\else(${asExpr(x[3])})   "


    fun expandTerm(operator: String, x: SList): String {
        return x.seq.subList(1, x.seq.size).joinToString(" $operator ") {
            "(${asExpr(it)})"
        }
    }
}
