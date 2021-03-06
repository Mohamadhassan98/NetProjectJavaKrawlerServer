package net.project

import crawler.FormCrawler
import edu.uci.ics.crawler4j.crawler.CrawlConfig
import edu.uci.ics.crawler4j.crawler.CrawlController
import edu.uci.ics.crawler4j.fetcher.PageFetcher
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer
import org.jsoup.Jsoup
import org.jsoup.UncheckedIOException
import utils.StaticAttributes.*
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


var crawlData = mutableMapOf<String, Boolean>()
var formActions = mutableSetOf<String>()

private fun getSitemap(url: String): List<String> {
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
            sites += getSitemap(it.html())
        }
    }
    siteMap.getElementsByTag("url").forEach {
        it.getElementsByTag("loc").forEach {
            sites += it.html()
        }
    }
    return sites
}

private fun getRobotsDisallowed(url: String) =
    URL("$url/robots.txt").readText().split("\n").filter { it.toLowerCase().contains("disallow") }
        .map { it.split(":")[1].trim() }.map { buildUrl(url, it) }.toSet()

fun getSiteMap(url: String) = getSitemap("$url/sitemap.xml").map {
    normalizeUrl(it)
}.map {
    buildUrl(url, it)
}.map { if (url.contains("https")) it.replace("http:", "https:") else it }

private fun startCrawler(
    baseUrl: String,
    initialSeed: List<String>,
    sitemap: Boolean,
    respectRobots: Boolean,
    includeExternals: Boolean
): CrawlController {
    val disallowed = if (respectRobots) {
        try {
            getRobotsDisallowed(baseUrl)
        } catch (ignored: Exception) {
            setOf()
        }
    } else setOf()
    val config = CrawlConfig()
    config.crawlStorageFolder = CRAWL_STORAGE_FOLDER
    // don't crawl links on page
    config.maxDepthOfCrawling = 0
    config.cleanupDelaySeconds = 2
    config.threadShutdownDelaySeconds = 2
    config.threadMonitoringDelaySeconds = 2
    config.isRespectNoIndex = false
    config.isRespectNoFollow = false
    config.isFollowRedirects = false
    config.isIncludeHttpsPages = true
    val pageFetcher = PageFetcher(config)
    val robotstxtConfig = RobotstxtConfig()
    val robotstxtServer = RobotstxtServer(robotstxtConfig, pageFetcher)
    val controller = CrawlController(config, pageFetcher, robotstxtServer)
    initialSeed.filterNot { it in disallowed }.forEach { controller.addSeed(it) }
    val factory = { FormCrawler(crawlData, disallowed, formActions, baseUrl, respectRobots, sitemap, includeExternals) }
    controller.startNonBlocking(factory, 8)
    return controller
}

fun crawlWithSiteMap(sitemap: List<String>, baseUrl: String) = startCrawler(
    baseUrl,
    sitemap,
    sitemap = true,
    respectRobots = false,
    includeExternals = false
)

fun deepCrawl(request: CrawlRequest) = startCrawler(
    normalizeUrl(request.url),
    listOf(normalizeUrl(request.url)),
    sitemap = false,
    respectRobots = request.respectRobots,
    includeExternals = request.includeExternals
)