import java.io.*;
import java.net. HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class SitemapChecker {

    private static final String SITEMAP_URL = "https://studyleo.com/sitemap.xml";

    // ⚙️ SİTEMAP KATEQORİYA FİLTRLƏRİ - true = yoxla, false = skip et
    private static final boolean CHECK_STATIC = false;           // /sitemaps/static/
    private static final boolean CHECK_UNIVERSITIES = true;    // /sitemaps/universities/
    private static final boolean CHECK_BLOGS = false;           // /sitemaps/blogs/
    private static final boolean CHECK_BLOG_TAGS = false;       // /sitemaps/blog-tags/
    private static final boolean CHECK_BLOG_CATEGORIES = false; // /sitemaps/blog-categories/
    private static final boolean CHECK_SEO_PAGES = false;        // /sitemaps/seo-pages/

    // ⚙️ DİL FİLTRLƏRİ (Əgər list boşdursa, bütün dillər yoxlanacaq. əgər müəyyən dillər göstərilərsə, yalnız onlar yoxlanacaq)
    // Məsələn: yalnız EN və RU yoxlamaq üçün:  {"en", "ru"}. Bütün dillərin id-ləri { "en","ar","ru", "fa", "ku", "az", "kk", "tr", "bg", "id", "de", "zh", "fr", "ky", "ur", "so", "tk", "uz", "sw"}
    private static final Set<String> LANGUAGE_FILTER = new HashSet<>(Arrays.asList(
            "ku","ur"
    ));

    private int totalChecked = 0;
    private int totalOK = 0;
    private int totalErrors = 0;
    private int totalSkipped = 0;
    private Set<String> processedSitemaps = new HashSet<>();
    private List<String> allResults = new ArrayList<>();
    private List<String> errorDetails = new ArrayList<>();

    private PrintWriter logWriter;
    private PrintWriter csvWriter;
    private String timestamp;

    public static void main(String[] args) {
        SitemapChecker checker = new SitemapChecker();
        checker.run();
    }

    public void run() {
        try {
            initializeLogFiles();

            printHeader();
            printTestConfig();

            System.out.println("🔍 Sitemap yoxlanır:  " + SITEMAP_URL);
            System.out.println("═". repeat(70) + "\n");

            logToFile("🔍 Sitemap yoxlanır: " + SITEMAP_URL);
            logToFile("═".repeat(70) + "\n");

            checkSitemap(SITEMAP_URL, 0);

            printSummary();

        } finally {
            closeLogFiles();
        }
    }

    private void printTestConfig() {
        System.out.println("⚙️  TEST KONFİQURASİYASI:");
        System.out. println("   📂 Yoxlanacaq kateqoriyalar:");
        if (CHECK_STATIC) System.out.println("      ✅ Static pages");
        if (CHECK_UNIVERSITIES) System.out.println("      ✅ Universities");
        if (CHECK_BLOGS) System.out.println("      ✅ Blogs");
        if (CHECK_BLOG_TAGS) System.out.println("      ✅ Blog Tags");
        if (CHECK_BLOG_CATEGORIES) System.out.println("      ✅ Blog Categories");
        if (CHECK_SEO_PAGES) System.out.println("      ✅ SEO Pages");

        if (! CHECK_STATIC || !CHECK_UNIVERSITIES || !CHECK_BLOGS ||
                !CHECK_BLOG_TAGS || !CHECK_BLOG_CATEGORIES || !CHECK_SEO_PAGES) {
            System.out.println("   ⏭️  Skip ediləcək:");
            if (! CHECK_STATIC) System.out.println("      ❌ Static pages");
            if (!CHECK_UNIVERSITIES) System.out.println("      ❌ Universities");
            if (!CHECK_BLOGS) System.out.println("      ❌ Blogs");
            if (!CHECK_BLOG_TAGS) System.out.println("      ❌ Blog Tags");
            if (!CHECK_BLOG_CATEGORIES) System.out.println("      ❌ Blog Categories");
            if (!CHECK_SEO_PAGES) System.out.println("      ❌ SEO Pages");
        }

        if (! LANGUAGE_FILTER.isEmpty()) {
            System.out.println("   🌐 Yoxlanacaq dillər:  " + String.join(", ", LANGUAGE_FILTER));
        } else {
            System.out. println("   🌐 Dil filtri:  Hamısı");
        }

        System.out.println();
    }

    private void initializeLogFiles() {
        try {
            timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            File logFile = new File("sitemap_check_" + timestamp + ".txt");
            logWriter = new PrintWriter(new FileWriter(logFile), true);

            File csvFile = new File("sitemap_check_" + timestamp + ".csv");
            csvWriter = new PrintWriter(new FileWriter(csvFile), true);
            csvWriter.println("Status,URL,Encoded URL,Error Message");

            System.out.println("📁 Log faylları yaradıldı:");
            System.out.println("   📄 " + logFile.getAbsolutePath());
            System.out.println("   📊 " + csvFile.getAbsolutePath());
            System.out.println();

        } catch (IOException e) {
            System.err.println("❌ Log faylları yaradıla bilmədi: " + e.getMessage());
        }
    }

    private void logToFile(String message) {
        if (logWriter != null) {
            logWriter.println(message);
        }
    }

    private void logToCsv(int statusCode, String url, String encodedUrl, String errorMsg) {
        if (csvWriter != null) {
            String escapedUrl = "\"" + url.replace("\"", "\"\"") + "\"";
            String escapedEncoded = "\"" + (encodedUrl != null ? encodedUrl.replace("\"", "\"\"") : "") + "\"";
            String escapedError = "\"" + (errorMsg != null ? errorMsg. replace("\"", "\"\"") : "") + "\"";

            csvWriter.println(statusCode + "," + escapedUrl + "," + escapedEncoded + "," + escapedError);
        }
    }

    private void closeLogFiles() {
        if (logWriter != null) {
            logWriter.close();
            System.out.println("\n✅ Log faylları saxlanıldı.");
        }
        if (csvWriter != null) {
            csvWriter.close();
        }
    }

    private void checkSitemap(String sitemapUrl, int depth) {
        if (processedSitemaps.contains(sitemapUrl)) {
            return;
        }
        processedSitemaps.add(sitemapUrl);

        // Sitemap-i yoxla və skip edilməlidirsə, skip et
        if (! sitemapUrl.equals(SITEMAP_URL) && shouldSkipSitemap(sitemapUrl)) {
            String msg = "⏭️  Skip edildi: " + sitemapUrl + " " + getSkipReason(sitemapUrl);
            System.out.println(msg);
            logToFile(msg);
            totalSkipped++;
            return;
        }

        try {
            String indent = "  ".repeat(depth);
            String message = indent + "📄 Sitemap açılır: " + sitemapUrl;
            System.out.println(message);
            logToFile(message);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(sitemapUrl);
            doc.getDocumentElement().normalize();

            // Alt-sitemap axtarışı
            NodeList sitemapNodes = doc.getElementsByTagName("sitemap");
            if (sitemapNodes.getLength() > 0) {
                int willCheck = 0;
                int willSkip = 0;

                // Statistika üçün say
                for (int i = 0; i < sitemapNodes.getLength(); i++) {
                    Element sitemapElement = (Element) sitemapNodes.item(i);
                    NodeList locInSitemap = sitemapElement. getElementsByTagName("loc");
                    if (locInSitemap.getLength() > 0) {
                        String subUrl = locInSitemap. item(0).getTextContent().trim();
                        if (shouldSkipSitemap(subUrl)) {
                            willSkip++;
                        } else {
                            willCheck++;
                        }
                    }
                }

                message = indent + "   ✓ Alt-sitemap sayı: " + sitemapNodes.getLength() +
                        " (yoxlanacaq: " + willCheck + ", skip: " + willSkip + ")";
                System.out. println(message);
                logToFile(message);

                for (int i = 0; i < sitemapNodes.getLength(); i++) {
                    Element sitemapElement = (Element) sitemapNodes.item(i);
                    NodeList locInSitemap = sitemapElement.getElementsByTagName("loc");

                    if (locInSitemap.getLength() > 0) {
                        String subSitemapUrl = locInSitemap.item(0).getTextContent().trim();

                        if (! shouldSkipSitemap(subSitemapUrl)) {
                            message = indent + "   ↳ Alt-sitemap: " + subSitemapUrl;
                            System.out.println(message);
                            logToFile(message);
                        }

                        checkSitemap(subSitemapUrl, depth + 1);
                    }
                }
            }

            // Səhifə linkləri
            NodeList urlNodes = doc.getElementsByTagName("url");
            if (urlNodes.getLength() > 0) {
                message = indent + "   ✓ Tapılan səhifə sayı: " + urlNodes.getLength();
                System.out. println(message);
                logToFile(message);

                for (int i = 0; i < urlNodes.getLength(); i++) {
                    Element urlElement = (Element) urlNodes.item(i);
                    NodeList locNodes = urlElement.getElementsByTagName("loc");

                    if (locNodes.getLength() > 0) {
                        String url = locNodes.item(0).getTextContent().trim();
                        checkUrl(url, depth);
                    }
                }
            }

        } catch (Exception e) {
            String error = "❌ Sitemap oxuma xətası: " + sitemapUrl + " - " + e.getMessage();
            System.err.println(error);
            logToFile(error);
        }
    }

    private boolean shouldSkipSitemap(String sitemapUrl) {
        // Ana sitemap heç vaxt skip olunmur
        if (sitemapUrl.equals(SITEMAP_URL)) {
            return false;
        }

        // Kateqoriya yoxlanışı
        if (sitemapUrl.contains("/sitemaps/static/") && ! CHECK_STATIC) return true;
        if (sitemapUrl.contains("/sitemaps/universities/") && !CHECK_UNIVERSITIES) return true;
        if (sitemapUrl.contains("/sitemaps/blogs/") && !CHECK_BLOGS) return true;
        if (sitemapUrl.contains("/sitemaps/blog-tags/") && !CHECK_BLOG_TAGS) return true;
        if (sitemapUrl.contains("/sitemaps/blog-categories/") && !CHECK_BLOG_CATEGORIES) return true;
        if (sitemapUrl.contains("/sitemaps/seo-pages/") && !CHECK_SEO_PAGES) return true;

        // Dil filtri (əgər təyin edilmişsə)
        if (!LANGUAGE_FILTER.isEmpty()) {
            String language = extractLanguage(sitemapUrl);
            if (language != null && !LANGUAGE_FILTER.contains(language)) {
                return true;
            }
        }

        return false;
    }

    private String extractLanguage(String sitemapUrl) {
        // URL-dən dil kodunu çıxar:  /sitemaps/xxx/en. xml -> "en"
        String[] parts = sitemapUrl.split("/");
        if (parts. length > 0) {
            String lastPart = parts[parts.length - 1]; // "en. xml"
            if (lastPart.endsWith(".xml")) {
                return lastPart.replace(".xml", ""); // "en"
            }
        }
        return null;
    }

    private String getSkipReason(String sitemapUrl) {
        if (sitemapUrl.contains("/sitemaps/static/") && !CHECK_STATIC) return "(static disabled)";
        if (sitemapUrl.contains("/sitemaps/universities/") && !CHECK_UNIVERSITIES) return "(universities disabled)";
        if (sitemapUrl.contains("/sitemaps/blogs/") && !CHECK_BLOGS) return "(blogs disabled)";
        if (sitemapUrl.contains("/sitemaps/blog-tags/") && !CHECK_BLOG_TAGS) return "(blog-tags disabled)";
        if (sitemapUrl.contains("/sitemaps/blog-categories/") && !CHECK_BLOG_CATEGORIES) return "(blog-categories disabled)";
        if (sitemapUrl.contains("/sitemaps/seo-pages/") && !CHECK_SEO_PAGES) return "(seo-pages disabled)";

        if (!LANGUAGE_FILTER.isEmpty()) {
            String lang = extractLanguage(sitemapUrl);
            if (lang != null && !LANGUAGE_FILTER.contains(lang)) {
                return "(language:  " + lang + " not in filter)";
            }
        }

        return "";
    }

    private void checkUrl(String url, int depth) {
        totalChecked++;
        String indent = "  ".repeat(depth + 1);

        int maxRetries = 2;
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            try {
                String encodedUrl = encodeUrl(url);

                HttpURLConnection connection = (HttpURLConnection) new URL(encodedUrl).openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(10000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Sitemap Checker)");

                int statusCode = connection.getResponseCode();

                String result = String.format("%s[%d] %s", indent, statusCode, url);

                if (! url.equals(encodedUrl)) {
                    result += "\n" + indent + "   🔗 Encoded: " + encodedUrl;
                }

                if (statusCode == 200) {
                    totalOK++;
                    String output = result + " ✅";
                    System.out. println(output);
                    logToFile(output);
                    logToCsv(statusCode, url, encodedUrl, null);
                    allResults.add(String.format("[%d] %s", statusCode, url));
                } else {
                    totalErrors++;
                    String output = result + " ⚠️";
                    System.out.println(output);
                    logToFile(output);
                    logToCsv(statusCode, url, encodedUrl, "Non-200 status");
                    allResults.add(String.format("[%d] %s", statusCode, url));
                    errorDetails.add(String.format("[%d] %s", statusCode, url));
                }

                connection.disconnect();
                Thread.sleep(50);
                return;

            } catch (java.net. SocketTimeoutException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    String retryMsg = indent + "⏱️ Timeout, yenidən cəhd " + retryCount + "/" + maxRetries + ": " + url;
                    System.out. println(retryMsg);
                    logToFile(retryMsg);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {}
                } else {
                    totalErrors++;
                    String error = String.format("%s[TIMEOUT] %s ❌", indent, url);
                    System.out.println(error);
                    logToFile(error);
                    logToCsv(0, url, null, "Timeout after " + maxRetries + " retries");
                    allResults.add(String.format("[TIMEOUT] %s", url));
                    errorDetails.add(String.format("[TIMEOUT] %s", url));
                }
            } catch (Exception e) {
                totalErrors++;
                String error = String.format("%s[ERROR] %s - %s ❌", indent, url, e. getMessage());
                System.out. println(error);
                logToFile(error);
                logToCsv(0, url, null, e.getMessage());
                allResults.add(String.format("[ERROR] %s - %s", url, e.getMessage()));
                errorDetails.add(String.format("[ERROR] %s - %s", url, e. getMessage()));
                return;
            }
        }
    }

    private String encodeUrl(String url) throws Exception {
        URL urlObj = new URL(url);

        String path = urlObj.getPath();
        String[] parts = path.split("/");
        StringBuilder encodedPath = new StringBuilder();

        for (String part : parts) {
            if (! part.isEmpty()) {
                String encoded = URLEncoder.encode(part, "UTF-8");
                encoded = encoded.replace("+", "%20");
                encodedPath.append("/").append(encoded);
            }
        }

        String query = urlObj.getQuery();
        String queryPart = (query != null) ? "?" + query : "";

        return urlObj.getProtocol() + "://" + urlObj.getHost() + encodedPath. toString() + queryPart;
    }

    private void printHeader() {
        String header = "\n" + "█". repeat(70) + "\n" +
                "█" + " ".repeat(68) + "█\n" +
                "█  🗺️  SITEMAP CHECKER - CONFIGURABLE MODE                        █\n" +
                "█" + " ".repeat(68) + "█\n" +
                "█". repeat(70) + "\n";
        System.out.println(header);
        logToFile(header);
    }

    private void printSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("\n").append("═".repeat(70)).append("\n");
        summary.append("📊 YEKUN NƏTİCƏ\n");
        summary.append("═".repeat(70)).append("\n");
        summary.append("   📌 Yoxlanan link sayı: ").append(totalChecked).append("\n");
        if (totalSkipped > 0) {
            summary.append("   ⏭️  Skip edilən sitemap sayı: ").append(totalSkipped).append("\n");
        }
        summary.append("   ✅ Uğurlu (200): ").append(totalOK).append("\n");
        summary.append("   ❌ Xətalı:  ").append(totalErrors).append("\n");

        double successRate = totalChecked > 0 ? (totalOK * 100.0 / totalChecked) : 0;
        summary.append("   📈 Uğur nisbəti: ").append(String.format("%.2f", successRate)).append("%\n");

        if (! errorDetails.isEmpty()) {
            summary.append("\n").append("─".repeat(70)).append("\n");
            summary.append("⚠️  XƏTALI LİNKLƏR:\n");
            summary.append("─".repeat(70)).append("\n");
            for (String error :  errorDetails) {
                summary. append("   ").append(error).append("\n");
            }
        }

        summary.append("\n").append("█".repeat(70)).append("\n");
        summary.append("█  ✅ YOXLAMA TAMAMLANDI!                                            █\n");
        summary.append("█".repeat(70)).append("\n");

        String summaryText = summary.toString();
        System.out.println(summaryText);
        logToFile(summaryText);
    }
}