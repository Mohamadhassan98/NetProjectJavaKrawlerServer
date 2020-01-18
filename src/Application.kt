package net.project

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import edu.uci.ics.crawler4j.crawler.CrawlController
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.pingPeriod
import io.ktor.http.cio.websocket.readText
import io.ktor.http.cio.websocket.timeout
import io.ktor.jackson.jackson
import io.ktor.routing.routing
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.delay
import java.io.File
import java.time.Duration

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

data class CrawlRequest(val url: String, val includeExternals: Boolean = false, val respectRobots: Boolean = true)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT) // Pretty Prints the JSON
            enableDefaultTyping()
        }
    }
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(60) // Disabled (null) by default
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE // Disabled (max value). The connection will be closed if surpassed this length.
        masking = false
    }
    lateinit var crawler: CrawlController
    routing {
        webSocket("/crawl") {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val requestText = frame.readText()
                        val request = jacksonObjectMapper().readValue<CrawlRequest>(requestText)
                        val siteMap = getSiteMap("${request.url}/sitemap.xml")
                        val data = jacksonObjectMapper().writerWithDefaultPrettyPrinter()
                            .writeValueAsString(mapOf("sitemap" to siteMap.isNotEmpty()))
                        outgoing.send(Frame.Text(data))
                        if (siteMap.isEmpty()) {
                            //start deep crawling
                        } else {
                            crawler = crawlWithSiteMap(siteMap)
                        }
                    }
                }
            }
        }
        webSocket("/result") {
            for (frame in incoming) {
                var page = 0
                while (crawlData.isEmpty());
                while (true) {
                    if (crawler.isFinished) {
                        val rem = crawlData.size % 10
                        if (crawlData.size >= (page + 1) * 10) {
                            val data = crawlData.toList().subList(page * 10, (page + 1) * 10)
                            val obj = data.map {
                                mapOf("url" to it, "hasForm" to false, "id" to it.hashCode())
                            }
                            val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                                mapOf("page" to page, "data" to obj)
                            )
                            page++
                            outgoing.send(Frame.Text(json))
                        } else if (rem > 0) {
                            val data = crawlData.toList().takeLast(rem)
                            val obj = data.map {
                                mapOf("url" to it, "hasForm" to false, "id" to it.hashCode())
                            }
                            val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                                mapOf("page" to page, "data" to obj)
                            )
                            outgoing.send(Frame.Text(json))
                            break
                        } else break
                        delay(1000)
                    } else {
                        if (crawlData.size >= (page + 1) * 10) {
                            val data = crawlData.toList().subList(page * 10, (page + 1) * 10)
                            val obj = data.map {
                                mapOf("url" to it, "hasForm" to false, "hash" to it.hashCode())
                            }
                            val json = jacksonObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(
                                mapOf("page" to page, "data" to obj)
                            )
                            page++
                            outgoing.send(Frame.Text(json))
                        }
                        delay(1000)
                    }
                }
            }
        }
        webSocket("/url") {
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val url = frame.readText()
                    val file = File("./data/html/${url}.html")
                    val text = file.readText()
                    outgoing.send(Frame.Text(text))
                }
            }
        }
    }
}