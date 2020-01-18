package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import utils.StaticAttributes;

import java.util.Set;

/**
 * Used to crawl sites with sitemap. No crawl to page links, no try to find forms.
 */
public class SimpleCrawler extends WebCrawler {

    private final Set<String> data;

    public SimpleCrawler(Set<String> data) {
        this.data = data;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().toLowerCase();
        System.out.println("Crawled: " + url);
        data.add(url);
        if (page.getParseData() instanceof HtmlParseData) {
            StaticAttributes.saveHtml(((HtmlParseData) page.getParseData()).getHtml(), url);
        }
    }
}
