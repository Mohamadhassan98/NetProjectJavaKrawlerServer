package crawler;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import utils.StaticAttributes;

import java.io.*;
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
    private final Pattern internal;
    private final boolean external;
    private final Map<String, Boolean> data;
    private final String baseUrl;
    private final boolean respectRobots;
    private final boolean hasSiteMap;
    private final Set<String> formActions;
    private final Set<String> disallowed;

    public FormCrawler(Map<String, Boolean> data, Set<String> disallowed, Set<String> formActions, String baseUrl, boolean respectRobots, boolean hasSiteMap, boolean external) {
        this.data = data;
        this.external = external;
        this.baseUrl = normalizeUrl(baseUrl);
        this.respectRobots = respectRobots;
        this.hasSiteMap = hasSiteMap;
        this.formActions = formActions;
        this.disallowed = disallowed;
        internal = Pattern.compile("(.*?)" + normalizeUrl(baseUrl).split("://")[1] + "/(.*)");
    }

    /**
     * Generates data based on name of tag, it's type and retrial time and fill into it
     *
     * @param input The input element to fill
     * @param k     the time of retrial, in range 0..10 (inclusive)
     */
    private void fillDataGenerate(Element input, int k) {
        String type = input.attr("type").toLowerCase();
        String name = input.attr("name");
        switch (type) {
            case "text":
            case "search":
                input.val(getRandomHyponym(name));
            case "date":
                input.val(StaticAttributes.randomDate.get(k));
            case "email":
                input.val(StaticAttributes.randomEmail.get(k));
            case "month":
                input.val(StaticAttributes.randomMonth.get(k));
            case "number":
                input.val(new Random().nextInt() + "");
            case "tel":
                input.val(StaticAttributes.randomTel.get(k));
            case "time":
                input.val(StaticAttributes.randomTime.get(k));
            case "week":
                input.val(StaticAttributes.randomWeek.get(k));
            default:
                input.val("haaale");
        }
    }

    private void enhancedForm(String htmlText, String url) {
        String normalizedUrl = normalizeUrl(url);
        Document html = Jsoup.parse(htmlText);
        try {
            //Get all forms in page
            Elements forms = html.select("form");
            data.put(url, !forms.isEmpty());
            if (forms.isEmpty()) {
                return;
            }
            // used to get cookies
            Connection.Response preRequest = Jsoup
                    .connect(normalizedUrl)
                    .userAgent(USER_AGENT)
                    .followRedirects(true)
                    .method(Connection.Method.GET)
                    .execute();
            forms.forEach(form -> {
                FormElement formElement = (FormElement) form;
                // extract action of form
                String action = normalizeUrl(form.attr("action"));
                // build full url of form action
                String requestUrl = buildUrl(action, normalizedUrl);
                if (formActions.contains(requestUrl)) return;
                else formActions.add(requestUrl);
                // extract name of form
                String id = form.attr("name");
                // extract all inputs of form
                Elements inputs = extractEmptyInputs(formElement);
                // repeat submit 10 times
                for (int i = 0; i < 10; i++) {
                    int finalI = i;
                    // for each input in form...
                    inputs.forEach(input -> fillDataGenerate(input, finalI));
                    Connection.Response response = submittingForm(formElement, preRequest.cookies());
                    if (response != null && response.statusCode() / 100 == 2) {
                        extractOutgoingUrls(response.body(), requestUrl).forEach(a -> getMyController().addSeed(a));
                        File file = new File("./data/form_data/" + rawUrl(baseUrl) + "/" + normalizedUrl.hashCode() + "/");
                        if (!file.exists()) {
                            file.mkdirs();
                        }
                        try (FileWriter fw = new FileWriter("./data/form_data/" + rawUrl(baseUrl) + "/" + normalizedUrl.hashCode() + "/" + id + ".html")) {
                            fw.write("for action " + requestUrl + " in page " + normalizedUrl + ":\n");
                            fw.write(response.statusCode() + "\n");
                            fw.write("with headers " + response.headers() + "\n");
                            fw.write("-----------------------------------------------\n");
                            fw.write(response.body());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // no need to more requests
                        break;
                    } else if (response != null) {
                        System.out.println("Couldn't crawl form, request failed with status code: " + response.statusCode());
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Elements extractEmptyInputs(FormElement form) {
        return form.select("input").not("[type=hidden]").not("[hidden]").not("[value~=(.+)]");
    }

    private String buildUrl(String url, String referer) {
        // if a url in href starts with / (e.g. "/login") then the full url will be
        // (e.g.) "lms.ui.ac.ir/login"
        // if it doesn't start with / (e.g. "login") then it will be appended to current page url
        // (e.g.) "lms.ui.ac.ir/accounts/login"
        if (isAbsoluteUrl(url)) {
            return url;
        }
        if (isRootRelativeUrl(url)) {
            return baseUrl + url;
        }
        return referer + "/" + url;
    }

    private Connection.Response submittingForm(FormElement form, Map<String, String> cookies) {
        try {
            return form.submit().cookies(cookies).followRedirects(true).execute();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String href = url.getURL().toLowerCase();
        boolean b = !FILTERS.matcher(href).matches() && data.get(href) == null && !disallowed.contains(href);
        if (!external) {
            return internal.matcher(href).matches() && b;
        }
        return b;
    }

    private Set<String> extractOutgoingUrls(String html, String url) {
        if (data.size() >= 200) {
            System.out.println("Maximum links exceeded!");
            return Set.of();
        }
        Document document = Jsoup.parse(html);
        return document
                .body()
                .getElementsByTag("a")
                .stream()
                .map(a -> a.attr("href"))
                .filter(a -> !a.isEmpty())
                .map(a -> buildUrl(a, url))
                .filter(a -> {
                    if (!external) {
                        return internal.matcher(a).matches();
                    }
                    return true;
                })
                .filter(a -> !data.containsKey(a))
                .filter(a -> !disallowed.contains(a))
                .collect(Collectors.toSet());
    }

    private boolean canIndex(String html) {
        if (!respectRobots) return true;
        Document document = Jsoup.parse(html);
        AtomicBoolean noIndex = new AtomicBoolean(false);
        document.head().getElementsByTag("meta").forEach(element -> {
            if (element.attr("name").equalsIgnoreCase("robots") && element.attr("content").contains("noindex")) {
                noIndex.set(true);
            }
        });
        return !noIndex.get();
    }

    private boolean canFollow(String html) {
        if (!respectRobots) return true;
        Document document = Jsoup.parse(html);
        AtomicBoolean noFollow = new AtomicBoolean(false);
        document.head().getElementsByTag("meta").forEach(element -> {
            if (element.attr("name").equalsIgnoreCase("robots") && element.attr("content").contains("nofollow")) {
                noFollow.set(true);
            }
        });
        return !noFollow.get();
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL().toLowerCase();
        System.out.println("Crawled: " + url);
        if (page.getParseData() instanceof HtmlParseData) {
            String html = ((HtmlParseData) page.getParseData()).getHtml();
            if (canIndex(html)) {
                saveHtml(html, url);
            }
            if (!hasSiteMap) {
                if (canFollow(html)) {
                    extractOutgoingUrls(html, url).forEach(a -> getMyController().addSeed(a));
                }
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