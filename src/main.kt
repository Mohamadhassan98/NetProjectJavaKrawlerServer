package net.project

import crawler.FormCrawler
import crawler.SimpleCrawler
import edu.uci.ics.crawler4j.crawler.CrawlConfig
import edu.uci.ics.crawler4j.crawler.CrawlController
import edu.uci.ics.crawler4j.fetcher.PageFetcher
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer
import org.jsoup.Jsoup
import org.jsoup.UncheckedIOException
import utils.StaticAttributes.CRAWL_STORAGE_FOLDER
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

var crawlData = mutableMapOf<String, Boolean>()

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

fun crawlWithSiteMap(sitemap: List<String>, baseUrl: String): CrawlController {
    val config = CrawlConfig()
    config.crawlStorageFolder = CRAWL_STORAGE_FOLDER
    // don't crawl links on page
    config.maxDepthOfCrawling = 0
    config.cleanupDelaySeconds = 2
    config.threadShutdownDelaySeconds = 2
    config.threadMonitoringDelaySeconds = 2

    val pageFetcher = PageFetcher(config)
    val robotstxtConfig = RobotstxtConfig()
    val robotstxtServer = RobotstxtServer(robotstxtConfig, pageFetcher)
    val controller = CrawlController(config, pageFetcher, robotstxtServer)
    sitemap.forEach {
        controller.addSeed(it)
    }
    val factory = { SimpleCrawler(crawlData, baseUrl) }
    controller.startNonBlocking(factory, 8)
    return controller
}

fun deepCrawl(request: CrawlRequest): CrawlController {
    val config = CrawlConfig()
    config.crawlStorageFolder = CRAWL_STORAGE_FOLDER
    config.maxDepthOfCrawling = 4
    config.cleanupDelaySeconds = 2
    config.threadShutdownDelaySeconds = 2
    config.threadMonitoringDelaySeconds = 2
    config.isRespectNoFollow = request.respectRobots
    config.isRespectNoIndex = request.respectRobots

    val pageFetcher = PageFetcher(config)
    val robotstxtConfig = RobotstxtConfig()
    val robotstxtServer = RobotstxtServer(robotstxtConfig, pageFetcher)
    val controller = CrawlController(config, pageFetcher, robotstxtServer)
    controller.addSeed(request.url)
    val factory = { FormCrawler(request.includeExternals, crawlData, request.url) }
    controller.startNonBlocking(factory, 8)
    return controller
}

fun main() {
//    println(getSiteMap("https://ktor.io/sitemap.xml").size)

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
