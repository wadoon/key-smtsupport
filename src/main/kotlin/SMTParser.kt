/**
 *
 * @author Alexander Weigl
 * @version 1 (8/19/25)
 */
class SMTLexer(val input: String, var startPosition: Int = 0) : Iterator<Token> {
    val RE_LPAREN = "\\(".toRegex()
    val RE_RPAREN = "\\)".toRegex()
    val RE_SYMBOL = "[^\\s()]+".toRegex()
    val RE_SYMBOL_COMPLEX = "#\\|.*?\\|".toRegex()
    val RE_WHITESPACE = "\\s+".toRegex()
    val RE_COMMENT = ";.*?\n".toRegex()

    val REGEX = listOf(
        RE_LPAREN to TokenType.LPAREN,
        RE_RPAREN to TokenType.RPAREN,
        RE_COMMENT to TokenType.COMMENT,
        RE_WHITESPACE to TokenType.WHITESPACE,
        RE_SYMBOL_COMPLEX to TokenType.SYMBOL,
        RE_SYMBOL to TokenType.SYMBOL,
    )

    override fun hasNext(): Boolean = startPosition < input.length

    override fun next(): Token {
        if (startPosition >= input.length)
            return Token(IntRange(startPosition, startPosition), TokenType.EOF, input)

        for ((re, tt) in REGEX) {
            val m = re.matchAt(input, startPosition)
            if (m != null) {
                startPosition = 1 + m.range.last
                println(m.groupValues)
                println(tt)
                return Token(m.range, tt, input)
            }
        }
        error("No regex matches ${input[startPosition]} at $startPosition")
    }
}

class SMTParser(input: String, startPosition: Int = 0) {
    val lexer = SMTLexer(input, startPosition)
    private var currentTokenPos = 0
    private lateinit var tokens: List<Token>

    fun parse(): List<SExpr> {
        tokens = lexer.asSequence()
            .filter { !it.type.isHidden() }
            .toList()

        println(tokens.map { it.text })
        val seq = arrayListOf<SExpr>()
        while (currentTokenPos < tokens.size) {
            readSymbol()?.let { seq.add(it) }
        }
        return seq
    }

    private fun peek() = tokens[currentTokenPos]
    private fun next() = tokens[consume()]
    private fun consume() =
        if (currentTokenPos - 2 < tokens.size) currentTokenPos++
        else currentTokenPos

    private fun readSymbol(): SExpr? {
        val it = next()
        return when (it.type) {
            TokenType.LPAREN -> {
                val seq = arrayListOf<SExpr>()
                while (peek().type != TokenType.RPAREN && peek().type != TokenType.EOF) {
                    readSymbol()?.let { seq.add(it) }
                }

                if (peek().type == TokenType.RPAREN)
                    consume()

                return SList(seq)
            }

            TokenType.SYMBOL -> SSymbol(it)
            else -> return null
        }
    }
}

data class Token(val range: IntRange, val type: TokenType, val input: String) {
    val text by lazy { input.substring(range) }
}

enum class TokenType { LPAREN, RPAREN, SYMBOL, NUMBER, WHITESPACE, EOF, COMMENT }

fun TokenType.isHidden(): Boolean = when (this) {
    TokenType.WHITESPACE, TokenType.COMMENT -> true
    else -> false
}
