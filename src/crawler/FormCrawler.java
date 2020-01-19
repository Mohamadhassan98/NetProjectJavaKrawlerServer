package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import utils.StaticAttributes;

import java.io.*;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static net.project.UtilsKt.getRandomHyponym;
import static utils.StaticAttributes.*;


public class FormCrawler extends WebCrawler {

    private final static Pattern FILTERS = Pattern.compile(".*(\\.(css|js|gif|jpg"
            + "|png|mp3|mp4|zip|gz|apk))$");

    private final boolean external;
    private final Map<String, Boolean> data;
    private final String baseUrl;
    private final boolean respectRobots;
    private final boolean hasSiteMap;

    public FormCrawler(boolean external, Map<String, Boolean> data, String baseUrl, boolean respectRobots, boolean hasSiteMap) {
        this.data = data;
        this.external = external;
        this.baseUrl = normalizeUrl(baseUrl);
        this.respectRobots = respectRobots;
        this.hasSiteMap = hasSiteMap;
    }

    /**
     * Generates data based on name of tag, it's type and retrial time.
     *
     * @param name the name of tag, used to extract hyponym from wordnet
     * @param type the type of tag
     * @param k    the time of retrial, in range 0..10 (inclusive)
     * @return a data to fill in form
     */
    private static String dataGenerate(String name, String type, int k) {
        switch (type.toLowerCase()) {
            case "text":
            case "search":
                return getRandomHyponym(name);
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

    private void enhancedForm(String htmlText, String url) {
        String normalizedUrl = normalizeUrl(url);
        Document html = Jsoup.parse(htmlText);
        try {
            //Get all forms in page
            Elements forms = html.getElementsByTag("form");
            data.put(url, !forms.isEmpty());
            forms.forEach(form -> {
                // extract action of form
                String action = normalizeUrl(form.attr("action"));
                // if a url in href starts with / (e.g. "/login") then the full url will be
                // (e.g.) "lms.ui.ac.ir/login"
                // if it doesn't start with / (e.g. "login") then it will be appended to current page url
                // (e.g.) "lms.ui.ac.ir/accounts/login"
                boolean isRootRelativeAction = isRootRelativeUrl(action);
                // absolute urls are non relative urls
                boolean absolute = isAbsoluteUrl(action);
                // extract name of form
                String id = form.attr("name");
                // extract method of form
                String method = form.attr("method");
                // extract all inputs of form
                Elements inputs = form.getElementsByTag("input");
                // build a map of (inputName -> inputValue)
                // used to submit form
                Map<String, String> params = new HashMap<>();
                // repeat submit 10 times
                for (int i = 0; i < 10; i++) {
                    int finalI = i;
                    // for each input in form...
                    inputs.forEach(input -> {
                        // extract name of input
                        String name = input.attr("name");
                        // extract type of input
                        String type = input.attr("type");
                        // put (name -> value) to map
                        params.put(name, dataGenerate(name, type, finalI));
                    });
                    // build full url of form action
                    String requestUrl = absolute ? action : (isRootRelativeAction ? baseUrl : (normalizedUrl + "/") + action);
                    switch (method.toLowerCase()) {
                        case "get": {
                            // if method is get, send get request with query strings to it
                            // e.g. send "lms.ui.ac.ir/login?username=ali&password=1234"
                            HttpResponse<String> response = requestGet(requestUrl, params);
                            // if request was successful, i.e. status code is from 200 family
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
                                // no need to more requests
                                break;
                            }
                        }
                        break;
                        case "post": {
                            // if method is post, send post request form data to it
                            // e.g. send "lms.ui.ac.ir/login" with json body
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

    private Set<String> extractOutgoingUrls(String html, String url) {
        Document document = Jsoup.parse(html);
        return document
                .body()
                .getElementsByTag("a")
                .stream()
                .map(a -> a.attr("href"))
                .filter(a -> !a.isEmpty())
                .filter(a -> {
                    if (!external) {
                        return a.startsWith(baseUrl);
                    }
                    return true;
                })
                .filter(a -> !data.containsKey(a))
                .map(a -> isAbsoluteUrl(a) ? a : (isRootRelativeUrl(a) ? (baseUrl + a) : (url + "/" + a)))
                .collect(Collectors.toSet());
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().toLowerCase();
        System.out.println("Crawled: " + url);
        if (page.getParseData() instanceof HtmlParseData) {
            String html = ((HtmlParseData) page.getParseData()).getHtml();
            if (respectRobots) {
                Document document = Jsoup.parse(html);
                AtomicBoolean noIndex = new AtomicBoolean(false);
                AtomicBoolean noFollow = new AtomicBoolean(false);
                document.head().getElementsByTag("meta").forEach(element -> {
                    if (element.attr("name").equalsIgnoreCase("robots") && element.attr("content").contains("noindex")) {
                        noIndex.set(true);
                    }
                    if (element.attr("name").equalsIgnoreCase("robots") && element.attr("content").contains("nofollow")) {
                        noFollow.set(true);
                    }
                });
                if (!noFollow.get() && !hasSiteMap) {
                    extractOutgoingUrls(html, url).forEach(a -> getMyController().addSeed(a));
                }
                if (!noIndex.get()) {
                    saveHtml(html, url);
                }
            } else {
                extractOutgoingUrls(html, url).forEach(a -> getMyController().addSeed(a));
                saveHtml(html, url);
            }
            enhancedForm(html, url);
        }
    }

    public void saveHtml(String html, String url) {
        String rawUrl = rawUrl(baseUrl);
        File baseFile = new File("./data/html/" + rawUrl + "/");
        if (!baseFile.exists()) {
            baseFile.mkdirs();
        }
        File file = new File("./data/html/" + rawUrl + "/" + url.hashCode() + ".html");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            try (FileOutputStream fos = new FileOutputStream(file); PrintWriter pw = new PrintWriter(fos)) {
                pw.print(html);
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}