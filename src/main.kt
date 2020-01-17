package net.project

import org.jsoup.Jsoup
import org.jsoup.UncheckedIOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun getSiteMap(url: String): List<String> {
    val client = HttpClient
        .newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    val request = HttpRequest
        .newBuilder()
        .GET()
        .uri(URI.create(url))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val siteMap = try {
        Jsoup.parse(response.body())
    } catch (e: UncheckedIOException) {
        println("couldn't parse xml: ${response.uri()}")
        Jsoup.parse("<html/>")
    }
    val sites = mutableListOf<String>()
    siteMap.getElementsByTag("sitemap").forEach {
        it.getElementsByTag("loc").forEach {
            sites += getSiteMap(it.html())
        }
    }
    siteMap.getElementsByTag("url").forEach {
        it.getElementsByTag("loc").forEach {
            sites += it.data()
        }
    }
    return sites
}

fun main() {
    println("________________________________________")
    getSiteMap("https://www.ui.ac.ir/sitemap.xml")
    println("________________________________________")
}