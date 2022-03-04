import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.engine.cio.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalTime
import java.util.*
import kotlin.math.pow

suspend fun main() {
    var totalLength = 5
    val use = listOf('р','о','т')
    val notUse = listOf('я','г','д','п','ы','ь')

    val startTime = LocalTime.now()
    val client = HttpClient(CIO) {
        expectSuccess = false
    }

    val notPos = mapOf(
        Pair('р', listOf(1)),
        Pair('о', listOf(2)),
        Pair('т', listOf(3)),
    )

    var totalRequests = 0
    totalLength--
    val all = mutableListOf('а','б','в','г','д','е','ё','ж','з','и','к','л','м','н','о','п','р','с','т','у','ф','х','ц','ч','ш','щ','ъ','ы','ь','э','ю','я')
    all.removeAll(notUse)

    var i = 0

    var max = 1
    for (j in 0..totalLength) {
        max *= all.size
    }

    val maxRequestInOnTime = max.toDouble().pow(1/3.toDouble()).toInt()

    val knownWords = mutableListOf<String>()

    while (i <= max) {
        val buildersList = mutableListOf<StringBuilder>()
        while (buildersList.size < maxRequestInOnTime) {
            i++
            val builder = StringBuilder()
            var index = i
            for (pos in 0..totalLength) {
                builder.append(all[index % all.size])
                index /= all.size
            }

            var allInGoodPos = true
            for (j in 0..totalLength) {
                val char = builder[j]
                if (notPos[char]?.contains(j) == true) {
                    allInGoodPos = false
                    break;
                }
            }

            if (allInGoodPos && builder.toList().containsAll(use)) {
                buildersList.add(builder)
            }
        }

        coroutineScope {
            buildersList.map { builder ->
                async {
                    val wordExistResponse =
                        client.get<String>("http://gramota.ru/slovari/dic/?bts=x&word=${builder}") {}.lowercase(Locale.getDefault())
                    totalRequests++
                    if (!wordExistResponse.contains("искомое слово отсутствует") && !knownWords.contains(builder.toString())) {
                        val partOfSpeechResponse = client.get<String>("https://wordius.ru/часть-речи/$builder/") {}.lowercase(Locale.getDefault())
                        totalRequests++
                        knownWords.add(builder.toString())
                        if (partOfSpeechResponse.contains("является существительным")) {
                            println(builder.toString().replace("ё", "е"))
                        } else {
                            println("${builder.toString().replace("ё", "е")} (не сущ., вроде)")
                        }
                    }
                }
            }
        }.awaitAll()
    }

    val endTime = LocalTime.now()
    println("it take ${endTime.toSecondOfDay() - startTime.toSecondOfDay()} seconds for $totalRequests requests")
}