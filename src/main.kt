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
    val client = HttpClient
        .newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    val request = HttpRequest
        .newBuilder()
        .GET()
        .uri(URI.create("http://lms.ui.ac.ir"))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    Jsoup.parse(response.body()).body().getElementsByTag("form").forEach {
        it.getElementsByTag("input").forEach {
            println(it.attr("type"))
        }
    }
}