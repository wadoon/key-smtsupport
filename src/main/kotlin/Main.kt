import java.nio.file.Paths

/**
 *
 * @author Alexander Weigl
 * @version 1 (8/19/25)
 */
object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val t = Translator()
        val files = t.smt2key(Paths.get(args.first()))
        println(files.first())
    }
}