package net.project

import net.didion.jwnl.JWNL
import net.didion.jwnl.data.POS
import net.didion.jwnl.data.PointerType
import net.didion.jwnl.dictionary.Dictionary
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.FormElement
import utils.StaticAttributes
import java.io.File
import java.io.FileInputStream

val dict: Dictionary by lazy {
    JWNL.initialize(FileInputStream(File("./lib/jwnl14-rc2/config/file_properties.xml")))
    Dictionary.getInstance()
}

fun String.getHyponyms() = dict.lookupIndexWord(POS.NOUN, this)
    ?.senses
    ?.flatMap {
        it.getPointers(PointerType.HYPONYM).flatMap {
            it.targetSynset.words.map {
                it.lemma
            }
        }
    }.orEmpty()

fun String.getRandomHyponym(): String {
    val hyponyms = getHyponyms()
    return if (hyponyms.isEmpty()) "abs" else hyponyms.random()
}

fun main() {
//    requestGet("http://lms.ui.ac.ir/login", mapOf())
//    println(jacksonObjectMapper().writeValueAsString(mapOf("username" to "953611133001", "password" to "1272591069")))
//    println(
//        requestPost(
//            "http://lms.ui.ac.ir/login",
//            mapOf("username" to "953611133001", "password" to "1272591069")
//        ).body()
//    )
//    val client = HttpClient
//        .newBuilder()
//        .followRedirects(HttpClient.Redirect.NORMAL)
//        .build()
//    val request = HttpRequest
//        .newBuilder()
//        .header("Content-Type", "application/x-www-form-urlencoded")
//        .POST(HttpRequest.BodyPublishers.ofString(jacksonObjectMapper().writeValueAsString(mapOf("username" to "953611133001", "password" to "1272591069"))))
//        .uri(URI.create("http://lms.ui.ac.ir/login"))
//        .build()
//    println(client.send(request, HttpResponse.BodyHandlers.ofString()).body())
    val preResponse = Jsoup
        .connect("http://lms.ui.ac.ir")
        .userAgent(StaticAttributes.USER_AGENT)
        .followRedirects(true)
        .method(Connection.Method.GET)
        .execute()
    val doc = preResponse.parse()
    val form = doc.selectFirst("form[action=/login]") as FormElement
//    println(form)
    form.select("input")
        .not("[type=hidden]")
        .not("[hidden]")
//        .not("[value]")
        .not("[value~=(.+)]")
        .forEachIndexed { index, element ->
            element.`val`(arrayOf("953611133001", "1272591069")[index])
//        println(element)
        }
    println(form.submit().cookies(preResponse.cookies()).userAgent(StaticAttributes.USER_AGENT).execute().body())
//        .not("[type=hidden]").not("[hidden=hidden]").select("[value~=^$]").forEach { println(it) }
//    val hiddens = doc.body().getElementsByTag("form").first { it.attr("action") == "/session" }.getElementsByTag("input").filter { it.attr("hidden") == "hidden" || it.attr("type") == "hidden" }.map { it.attr("name") }
//    val hiddens = preResponse.parse().body().getElementsByTag("form").first { it.attr("action") == "/session" }
//        .getElementsByTag("input").filter { it.attr("type") == "hidden" }.map { it.attr("") }
//    val hid = doc.body().allElements.forms().first { it.attr("action") == "/session" }.formData().filter { it.value().isBlank() }.filter { it.key() !in hiddens }.forEach { println("Key: ${it.key()}, val: ${it.value()}") }
//    val connectMethod = Connection.Method.POST
//    val response = Jsoup
//        .connect("https://www.github.com/session")
//        .userAgent(StaticAttributes.USER_AGENT)
//        .followRedirects(true)
//        .data(mapOf("login" to "emohamadhassan@gmail.com", "password" to "Saga9130172688"))
//        .method(connectMethod)
//        .cookies(preResponse.cookies())
//        .execute()
//    println(response.body())
}