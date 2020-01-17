package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.url.WebURL;

public class NetCrawler extends WebCrawler {

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onBeforeExit() {
        super.onBeforeExit();
    }

    @Override
    protected void onRedirectedStatusCode(Page page) {
        super.onRedirectedStatusCode(page);
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        return super.shouldVisit(referringPage, url);
    }

    @Override
    public void visit(Page page) {
        super.visit(page);
    }
}
