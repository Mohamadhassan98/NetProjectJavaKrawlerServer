package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;

import java.util.Map;

public class SimpleCrawler extends WebCrawler {

    private final Map<String, String> data;

    public SimpleCrawler(Map<String, String> data) {
        this.data = data;
    }

    @Override
    public void visit(Page page) {
        System.out.println(data.size());
        if (page.getParseData() instanceof HtmlParseData) {
            data.put(page.getWebURL().getURL(), ((HtmlParseData) page.getParseData()).getHtml());
        }
    }
}
