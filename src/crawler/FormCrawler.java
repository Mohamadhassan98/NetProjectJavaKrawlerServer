package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import net.project.UtilsKt;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import utils.StaticAttributes;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;


public class FormCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz))$");

    private final boolean external;
    private final Map<String, Boolean> data;
    private final String baseUrl;

    public FormCrawler(boolean external, Map<String, Boolean> data, String baseUrl) {
        this.data = data;
        this.external = external;
        this.baseUrl = baseUrl;
    }

    public void form(String htmlText, String url) {
        Document html = Jsoup.parse(htmlText);
        try {
            Elements form = html.getElementsByTag("form");
            List<FormElement> forms = form.forms();
            data.put(url, !form.isEmpty());
            for (int i = 0; i < forms.size(); i++) {
                FormElement formElement = forms.get(i);
                String action = formElement.attributes().get("action");
                String id = formElement.id();
                String method = formElement.attributes().get("method");

                // for select random word from wordNetLists and try to submitting
                for (int k = 0; k < 10; k++) {
                    List<Connection.KeyVal> inputs = formElement.formData();
                    for (int j = 0; j < inputs.size(); j++) {
                        Connection.KeyVal input = inputs.get(j);
                        String key = input.key();
                        Element eTemp = formElement
                                .selectFirst("#" + key);
//                        System.out.println("Etemp: " + eTemp + "for key: " + key);
                        String wordTemp;
                        switch (input.key("type").value()) {
                            case "text":
                            case "search":
                                wordTemp = UtilsKt.getRandomHyponym(key);
                                eTemp.val(wordTemp.isEmpty() ? "abs" : wordTemp);
                                break;
                            case "date":
                                eTemp.val(StaticAttributes.randomDate.get(k));
                                break;
                            case "email":
                                eTemp.val(StaticAttributes.randomEmail.get(k));
                                break;
                            case "month":
                                eTemp.val(StaticAttributes.randomMonth.get(k));
                                break;
                            case "number":
                                eTemp.val(new Random().nextInt() + "");
                                break;
                            case "tel":
                                eTemp.val(StaticAttributes.randomTel.get(k));
                                break;
                            case "time":
                                eTemp.val(StaticAttributes.randomTime.get(k));
                                break;
                            case "week":
                                eTemp.val(StaticAttributes.randomWeek.get(k));
                                break;
                            default:
                                eTemp.val("haaale");
                                break;
                        }


                        // test of login page lms and ok
//                        forms.get(i).selectFirst("#username").val("953611133003");
//                        forms.get(i).selectFirst("#password").val("3920672771");
                    }
                    if (submittingForm(forms.get(i), method, action))
                        break;
                }
                String normalizedBaseUrl = baseUrl.split("://")[1].split("/")[0];
                File file = new File("./data/form/" + normalizedBaseUrl + "/");
                if (!file.exists()) {
                    file.mkdirs();
                }
                FileWriter fw = new FileWriter(
                        "./data/form/" + normalizedBaseUrl + "/" + id + ".html"
                );
                fw.write(forms.get(i).attributes().html());
                fw.write(forms.get(i).html());
                fw.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("Success...");

    }

    public boolean submittingForm(FormElement form, String method, String action) {
        try {
            String formUrl = StaticAttributes.baseUrl + action.substring(1);
            Connection.Method connectMethod = method.toUpperCase().equals("POST") ?
                    Connection.Method.POST : Connection.Method.GET;

            Connection.Response goToFormPage = Jsoup.connect(formUrl)
                    .userAgent(StaticAttributes.USER_AGENT)
                    .method(Connection.Method.GET)
                    .execute();

            Connection.Response connection = Jsoup
                    .connect(formUrl)
                    .userAgent(StaticAttributes.USER_AGENT)
                    .cookies(goToFormPage.cookies())
                    .data(form.formData())
                    .method(connectMethod)
                    .execute();

//            System.out.println(connection.url());

            if (!connection.url().toString().equals(formUrl)) {
                getMyController().addSeed(connection.url().toString());
                return true;
            } else return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        boolean b = !FILTERS.matcher(href).matches() && data.get(href) == null;
        if (!external) {
            return href.startsWith(baseUrl) && b;
        }
        return b;
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().toLowerCase();
        System.out.println("Crawled: " + url);
        if (page.getParseData() instanceof HtmlParseData) {
            StaticAttributes.saveHtml(((HtmlParseData) page.getParseData()).getHtml(), url, baseUrl);
        }

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();
            form(html, url);
        }
    }
}