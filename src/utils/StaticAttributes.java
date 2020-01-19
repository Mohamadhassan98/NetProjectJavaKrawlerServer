package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StaticAttributes {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
    public static final String CRAWL_STORAGE_FOLDER = "./data/crawl/root";

    public static final List<String> randomDate = Arrays.asList(
            "2019-05-05",
            "2020-10-05",
            "1999-11-05",
            "2015-06-15",
            "2000-12-09",
            "2022-03-14",
            "2005-05-05",
            "2019-06-24",
            "2017-10-26",
            "2004-12-01"
    );

    public static final List<String> randomMonth = Arrays.asList(
            "2019-12",
            "2020-10",
            "1999-11",
            "2015-03",
            "2000-12",
            "2022-08",
            "2005-05",
            "2019-01",
            "2017-10",
            "2004-12"
    );

    public static final List<String> randomWeek = Arrays.asList(
            "2019-W05",
            "2020-W15",
            "1999-W23",
            "2015-W08",
            "2000-W51",
            "2022-W29",
            "2005-W34",
            "2019-W45",
            "2017-W16",
            "2004-W10"
    );

    public static final List<String> randomTime = Arrays.asList(
            "00:00",
            "21:21",
            "15:32",
            "10:55",
            "11:02",
            "07:15",
            "05:05",
            "16:20",
            "17:54",
            "18:00"
    );

    public static final List<String> randomEmail = Arrays.asList(
            "ali98@yahoo.com",
            "hossein76@yahoo.com",
            "abc@gmail.com",
            "hassan@gmail.com",
            "okbye@outlook.com",
            "khobi1386@outlook.com",
            "random@live.com",
            "okeye75@live.com",
            "zahra1985@hotmail.com",
            "zafaran65@hotmail.com"
    );

    public static final List<String> randomTel = Arrays.asList(
            "988132173676",
            "983152369871",
            "15557271428",
            "16536589662",
            "982185369426",
            "39366555770",
            "988132175826",
            "33655564919",
            "989155587587",
            "989226521258"
    );


    public static void saveHtml(String html, String url, String baseUrl) {
        String normalizedBaseUrl = baseUrl.split("://")[1].split("/")[0];
        File baseFile = new File("./data/html/" + normalizedBaseUrl + "/");
        if (!baseFile.exists()) {
            baseFile.mkdirs();
        }
        File file = new File("./data/html/" + normalizedBaseUrl + "/" + url.hashCode() + ".html");
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

    public static void clearData(Map<String, Boolean> data, Set<String> formActions) {
        data.clear();
        formActions.clear();
    }

    public static String normalizeUrl(String url) {
        String result = url.toLowerCase();
        if (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    public static boolean isRootRelativeUrl(String url) {
        return url.startsWith("/");
    }

    public static boolean isAbsoluteUrl(String url) {
        try {
            return new URI(url).isAbsolute();
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    public static HttpResponse<String> requestGet(String url, Map<String, String> params) {
        String actualUrl = url + "?" + params.entrySet().stream().map(param -> param.getKey() + "=" + param.getValue()).collect(Collectors.joining("&"));
        HttpClient client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest
                .newBuilder()
                .GET()
                .uri(URI.create(actualUrl))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpResponse<String> requestPost(String url, Map<String, String> params) {
        HttpClient client = HttpClient
                .newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest
                .newBuilder()
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(params.entrySet().stream().map(param -> param.getKey() + "=" + param.getValue()).collect(Collectors.joining("&"))))
                .uri(URI.create(url))
                .build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String rawUrl(String url) {
        return url.split("://")[1].split("/")[0];
    }
}
