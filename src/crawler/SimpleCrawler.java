package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import utils.StaticAttributes;

import java.util.Map;

/**
 * Used to crawl sites with sitemap. No crawl to page links, no try to find forms.
 */
public class SimpleCrawler extends WebCrawler {

    private final Map<String, Boolean> data;
    private final String baseUrl;

    public SimpleCrawler(Map<String, Boolean> data, String baseUrl) {
        this.data = data;
        this.baseUrl = baseUrl;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().toLowerCase();
        System.out.println("Crawled: " + url);
        data.put(url, false);
        if (page.getParseData() instanceof HtmlParseData) {
            StaticAttributes.saveHtml(((HtmlParseData) page.getParseData()).getHtml(), url, baseUrl);
        }
    }
}
