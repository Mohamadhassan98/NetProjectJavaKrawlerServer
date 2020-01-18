package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import net.project.UtilsKt;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import utils.StaticAttributes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import static utils.StaticAttributes.*;


public class FormCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz|apk))$");

    private final boolean external;
    private final Map<String, Boolean> data;
    private final String baseUrl;

    public FormCrawler(boolean external, Map<String, Boolean> data, String baseUrl) {
        this.data = data;
        this.external = external;
        this.baseUrl = normalizeUrl(baseUrl);
    }

    private static String dataGenerate(String name, String type, int k) {
        switch (type.toLowerCase()) {
            case "text":
            case "search":
                return UtilsKt.getRandomHyponym(name);
            case "date":
                return StaticAttributes.randomDate.get(k);
            case "email":
                return StaticAttributes.randomEmail.get(k);
            case "month":
                return StaticAttributes.randomMonth.get(k);
            case "number":
                return new Random().nextInt() + "";
            case "tel":
                return StaticAttributes.randomTel.get(k);
            case "time":
                return StaticAttributes.randomTime.get(k);
            case "week":
                return StaticAttributes.randomWeek.get(k);
            default:
                return "haaale";
        }
    }

    public void enhancedForm(String htmlText, String url) {
        String normalizedUrl = normalizeUrl(url);
        Document html = Jsoup.parse(htmlText);
        try {
            Elements forms = html.getElementsByTag("form");
            data.put(url, !forms.isEmpty());
            forms.forEach(form -> {
                String action = normalizeUrl(form.attr("action"));
                boolean absolute = isAbsoluteUrl(action);
                String id = form.attr("name");
                String method = form.attr("method");
                Elements inputs = form.getElementsByTag("input");
                Map<String, String> params = new HashMap<>();
                for (int i = 0; i < 10; i++) {
                    int finalI = i;
                    inputs.forEach(input -> {
                        String name = input.attr("name");
                        String type = input.attr("type");
                        params.put(name, dataGenerate(name, type, finalI));
                    });
                    String requestUrl = absolute ? action : normalizedUrl + "/" + action;
                    switch (method.toLowerCase()) {
                        case "get": {
                            HttpResponse<String> response = requestGet(requestUrl, params);
                            if (response != null && response.statusCode() / 100 == 2) {
                                File file = new File("./data/form_data/" + rawUrl(baseUrl) + "/" + normalizedUrl.hashCode() + "/");
                                if (!file.exists()) {
                                    file.mkdirs();
                                }
                                try (FileWriter fw = new FileWriter("./data/form_data/" + rawUrl(baseUrl) + "/" + normalizedUrl.hashCode() + "/" + id + ".html")) {
                                    fw.write(response.body());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                        break;
                        case "post": {
                            HttpResponse<String> response = requestPost(requestUrl, params);
                            if (response != null && response.statusCode() / 100 == 2) {
                                File file = new File("./data/form_data/" + rawUrl(baseUrl) + "/" + normalizedUrl.hashCode() + "/");
                                if (!file.exists()) {
                                    file.mkdirs();
                                }
                                try (FileWriter fw = new FileWriter("./data/form_data/" + rawUrl(baseUrl) + "/" + normalizedUrl.hashCode() + "/" + id + ".html")) {
                                    fw.write(response.body());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void form(String htmlText, String url) {
//        Document html = Jsoup.parse(htmlText);
//        try {
//            Elements form = html.getElementsByTag("form");
//            List<FormElement> forms = form.forms();
//            data.put(url, !form.isEmpty());
//
//            for (int i = 0; i < forms.size(); i++) {
//                String action = forms.get(i).attributes().get("action");
//                String id = form.get(i).id();
//                String method = forms.get(i).attributes().get("method");
//                Set<Element> elementList = new LinkedHashSet<>(forms.get(i).getElementsByTag("input"));
//
//
//                System.out.println(elementList.size());
//
//                // for select random word from wordNetLists and try to submitting
//                for (int k = 0; k < 10; k++) {
//                    List<Connection.KeyVal> inputs = forms.get(i).formData();
//                    for (int j = 0; j < inputs.size(); j++) {
//                        String key = inputs.get(j).key();
//                        String type = "";
//                        Element elementTemp = (Element) elementList.toArray()[j];
//                        if (elementTemp.attr("name").equals(key))
//                            type = elementTemp.attr("type");
//                        Element eTemp = forms.get(i)
//                                .selectFirst("#" + key);
//                        String wordTemp;
//                        System.out.println(type);
//                        switch (type) {
//                            case "text":
//                            case "search":
//                                wordTemp = UtilsKt.getRandomHyponym(inputs.get(j).key());
//                                eTemp.val(wordTemp.isEmpty() ? "abs" : wordTemp);
//                                break;
//                            case "date":
//                                eTemp.val(StaticAttributes.randomDate.get(k));
//                                break;
//                            case "email":
//                                eTemp.val(StaticAttributes.randomEmail.get(k));
//                                break;
//                            case "month":
//                                eTemp.val(StaticAttributes.randomMonth.get(k));
//                                break;
//                            case "number":
//                                eTemp.val(new Random().nextInt() + "");
//                                break;
//                            case "tel":
//                                eTemp.val(StaticAttributes.randomTel.get(k));
//                                break;
//                            case "time":
//                                eTemp.val(StaticAttributes.randomTime.get(k));
//                                break;
//                            case "week":
//                                eTemp.val(StaticAttributes.randomWeek.get(k));
//                                break;
//                            default:
//                                eTemp.val("haaale");
//                                break;
//                        }
//                    }
//                    if (submittingForm(forms.get(i), method, action))
//                        break;
//                }
//                String normalizedBaseUrl = baseUrl.split("://")[1].split("/")[0];
//                File file = new File("./data/form/" + normalizedBaseUrl + "/");
//                if (!file.exists()) {
//                    file.mkdirs();
//                }
//                FileWriter fw = new FileWriter(
//                        "./data/form/" + normalizedBaseUrl + "/" + id + ".html"
//                );
//                fw.write(forms.get(i).attributes().html());
//                fw.write(forms.get(i).html());
//                fw.close();
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        System.out.println("Success...");
//
//    }

//    public boolean submittingForm(FormElement form, String method, String action) {
//        try {
//            String formUrl = baseUrl + action;
//            Connection.Method connectMethod = method.toUpperCase().equals("POST") ?
//                    Connection.Method.POST : Connection.Method.GET;
//
//            Connection.Response goToFormPage = Jsoup.connect(formUrl)
//                    .userAgent(StaticAttributes.USER_AGENT)
//                    .method(Connection.Method.GET)
//                    .execute();
//
//            Connection.Response connection = Jsoup
//                    .connect(formUrl)
//                    .userAgent(StaticAttributes.USER_AGENT)
//                    .cookies(goToFormPage.cookies())
//                    .data(form.formData())
//                    .method(connectMethod)
//                    .execute();
//
//            System.out.println(connection.url());
//
//            if (!connection.url().toString().equals(formUrl)) {
//                getMyController().addSeed(connection.url().toString());
//                return true;
//            } else return false;
//        } catch (Exception e) {
//            e.printStackTrace();
//            return false;
//        }
//
//    }

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
            enhancedForm(html, url);
        }
    }
}