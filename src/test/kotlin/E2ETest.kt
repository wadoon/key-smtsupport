import de.uka.ilkd.key.nparser.ParsingFacade
import org.antlr.v4.runtime.CharStreams
import org.junit.jupiter.api.RepeatedTest
import kotlin.test.assertEquals


/**
 *
 * @author Alexander Weigl
 * @version 1 (8/19/25)
 */
class E2ETest {
    @RepeatedTest(100)
    fun test_a() = test("a.smt2", "a.key")

    private fun test(smt: String, key: String) {
        val smt = readResourceText(smt)
        val expected = readResourceText(key)

        val t = Translator()
        val actual = t.smt2key(smt).first()

        assertEquals(expected, actual)

        ParsingFacade.parseFile(CharStreams.fromString(actual))
    }
}

private fun E2ETest.readResourceText(key: String): String =
    javaClass.getResourceAsStream(key)?.bufferedReader(Charsets.UTF_8)?.readText() ?: "not found"