package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import net.project.UtilsKt;
import org.jsoup.Connection;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import utils.StaticAttributes;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class FormCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz))$");

    /**
     * This method receives two parameters. The first parameter is the page
     * in which we have discovered this new url and the second parameter is
     * the new url. You should implement this function to specify whether
     * the given url should be crawled or not (based on your crawling logic).
     * In this example, we are instructing the crawler to ignore urls that
     * have css, js, git, ... extensions and to only accept urls that start
     * with "https://www.ics.uci.edu/". In this case, we didn't need the
     * referringPage parameter to make the decision.
     */
    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        return !FILTERS.matcher(href).matches();
//                && href.startsWith("https://www.ics.uci.edu/");
    }

    /**
     * This function is called when a page is fetched and ready
     * to be processed by your program.
     */
    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        System.out.println("URL: " + url);

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String text = htmlParseData.getText();
            String html = htmlParseData.getHtml();
            Set<WebURL> links = htmlParseData.getOutgoingUrls();

            form(html);

            System.out.println("Text length: " + text.length());
            System.out.println("Html length: " + html.length());
            System.out.println("Number of outgoing links: " + links.size());
        }
    }

    public static void form(String htmlText) {

        Document html = Jsoup.parse(htmlText);
        try {
            Elements form = html.getElementsByTag("form");
            List<FormElement> forms = form.forms();

            for (int i = 0; i < forms.size(); i++) {
                String action = forms.get(i).attributes().get("action");
                String id = form.get(i).id();
                String method = forms.get(i).attributes().get("method");

                List<Connection.KeyVal> inputs = forms.get(i).formData();
                List<List<String>> wordNetList = new ArrayList<>();

                Random r = new Random();
                int min = 500;
                for (int j = 0; j < inputs.size(); j++) {
                    wordNetList.add(UtilsKt.getHyponyms(inputs.get(j).key()));
                    int temp = wordNetList.get(i).size();
                    if (temp < min && temp != 0) {
                        r.nextInt(temp);
                        min = temp;
                    } else {
                        System.out.println("No similar word found");
                        break;
                    }
                }


                // for select random word from wordNetLists and try to submiting
                if (min != 500)
                    for (int k = 0; k < 10; k++) {
                        for (int j = 0; j < inputs.size(); j++) {
                            forms.get(i)
                                    .selectFirst("#" + inputs.get(j).key())
                                    .val(wordNetList.get(j).get(r.nextInt()));


                            // test of login page lms and ok
//                        forms.get(i).selectFirst("#username").val("953611133003");
//                        forms.get(i).selectFirst("#password").val("3920672771");
                        }
                        if (submitingForm(forms.get(i), method, action))
                            break;
                    }
                FileWriter fw = new FileWriter(
                        "./dataFileOfPageCrawl/" + id + ".html"
                );
                fw.write(forms.get(i).attributes().html());
                fw.write(forms.get(i).html());
                fw.close();
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        System.out.println("Success...");


    }

    public static boolean submitingForm(FormElement form, String method, String action) {
        try {
            String loginUrl = StaticAttributes.baseUrl + action.substring(1);
            Connection.Method connectMethod = method.toUpperCase().equals("POST") ?
                    Connection.Method.POST : Connection.Method.GET;

            Connection.Response goToLoginPage = Jsoup.connect(loginUrl)
                    .userAgent(StaticAttributes.USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();

            Connection.Response connection = Jsoup
                    .connect(loginUrl)
                    .userAgent(StaticAttributes.USER_AGENT)
                    .cookies(goToLoginPage.cookies())
                    .data(form.formData())
                    .method(connectMethod)
                    .execute();

            System.out.println(connection.url());

            if (connection.url().equals(loginUrl))
                return false;
            else
                return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}