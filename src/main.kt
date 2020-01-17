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
            sites += it.html()
        }
    }
    return sites
}

fun indexSite(url: String): String {
    val client = HttpClient
        .newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    val request = HttpRequest
        .newBuilder()
        .GET()
        .uri(URI.create(url))
        .build()
    return client.send(request, HttpResponse.BodyHandlers.ofString()).body()
}

fun main() {
    "username".getHyponyms().forEach {
        println(it)
    }
}

/*
fun main() {
    println("________________________________________")
    val siteMap = getSiteMap("https://ktor.io/sitemap.xml")
    println(siteMap)
    println("________________________________________")
    println(siteMap.size)
    val crawlStorageFolder = "./data/crawl/root"
    val numberOfCrawlers = 8

    val config = CrawlConfig()
    config.crawlStorageFolder = crawlStorageFolder
    config.maxDepthOfCrawling = 0
    config.politenessDelay = 200
    // Instantiate the controller for this crawl.
    // Instantiate the controller for this crawl.
    val pageFetcher = PageFetcher(config)
    val robotstxtConfig = RobotstxtConfig()
    val robotstxtServer = RobotstxtServer(robotstxtConfig, pageFetcher)
    val controller = CrawlController(config, pageFetcher, robotstxtServer)

    // For each crawl, you need to add some seed urls. These are the first
    // URLs that are fetched and then the crawler starts following links
    // which are found in these pages

    // For each crawl, you need to add some seed urls. These are the first
    // URLs that are fetched and then the crawler starts following links
    // which are found in these pages
    siteMap.forEach {
        controller.addSeed(it)
    }

    // The factory which creates instances of crawlers.

    // The factory which creates instances of crawlers.
    val result = mutableMapOf<String, String>()
    val factory = WebCrawlerFactory { SimpleCrawler(result) }

    // Start the crawl. This is a blocking operation, meaning that your code
    // will reach the line after this only when crawling is finished.

    // Start the crawl. This is a blocking operation, meaning that your code
    // will reach the line after this only when crawling is finished.
    controller.start(factory, numberOfCrawlers)
}*/
