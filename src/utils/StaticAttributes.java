package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

public class StaticAttributes {
    public static final String baseUrl = "http://lms.ui.ac.ir/";
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";
    public static final String CRAWL_STORAGE_FOLDER = "./data/crawl/root";

    public static void saveHtml(String html, String url) {
        File file = new File("./data/html/" + url.hashCode() + ".html");
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
