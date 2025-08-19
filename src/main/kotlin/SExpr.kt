import java.util.function.IntFunction

sealed class SExpr {}
data class SSymbol(val symbol: Token) : SExpr()
data class SList(val seq: MutableList<SExpr> = ArrayList()) : SExpr(), MutableList<SExpr> by seq