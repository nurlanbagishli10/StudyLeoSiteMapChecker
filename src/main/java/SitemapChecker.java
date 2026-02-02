import java.io.*;
import java.net. HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
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
       "en"
    ));

    // ⚙️ MULTI-THREADING KONFİQURASİYASI
    private static final int THREAD_COUNT = 10;           // Paralel thread sayı
    private static final int MAX_CONCURRENT_REQUESTS = 10; // Eyni anda maksimum sorğu sayı

    // ⚙️ LOG QOVLUQ KONFİQURASİYASI
    private static final String LOG_DIRECTORY = "logs";  // Logların saxlanacağı qovluq

    private AtomicInteger totalChecked = new AtomicInteger(0);
    private AtomicInteger totalOK = new AtomicInteger(0);
    private AtomicInteger totalErrors = new AtomicInteger(0);
    private AtomicInteger totalSkipped = new AtomicInteger(0);
    private Set<String> processedSitemaps = new HashSet<>();
    private ConcurrentLinkedQueue<String> allResults = new ConcurrentLinkedQueue<>();
    private ConcurrentLinkedQueue<String> errorDetails = new ConcurrentLinkedQueue<>();

    private ExecutorService executor;
    private Semaphore rateLimiter;

    private PrintWriter logWriter;
    private PrintWriter csvWriter;
    private String timestamp;

    public static void main(String[] args) {
        SitemapChecker checker = new SitemapChecker();
        checker.run();
    }

    public void run() {
        try {
            // İnisializasiya threading komponentləri
            executor = Executors.newFixedThreadPool(THREAD_COUNT);
            rateLimiter = new Semaphore(MAX_CONCURRENT_REQUESTS);

            initializeLogFiles();

            printHeader();
            printTestConfig();

            System.out.println("🔍 Sitemap yoxlanır:  " + SITEMAP_URL);
            System.out.println("═". repeat(70) + "\n");

            logToFile("🔍 Sitemap yoxlanır: " + SITEMAP_URL);
            logToFile("═".repeat(70) + "\n");

            checkSitemap(SITEMAP_URL, 0);

            // Bütün task-ların bitməsini gözlə
            executor.shutdown();
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                System.err.println("⚠️ Bəzi task-lar vaxtında bitmədi, zorla bağlanır...");
                executor.shutdownNow();
            }

            printSummary();

        } catch (InterruptedException e) {
            System.err.println("❌ İcra kəsildi: " + e.getMessage());
            executor.shutdownNow();
        } finally {
            closeLogFiles();
        }
    }

    private void printTestConfig() {
        synchronized (System.out) {
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

            System.out.println("   🧵 Thread sayı: " + THREAD_COUNT);
            System.out.println("   🔒 Maksimum eyni anda sorğu: " + MAX_CONCURRENT_REQUESTS);

            System.out.println();
        }
    }

    private void initializeLogFiles() {
        try {
            timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());

            // Log qovluğunu yarat (əgər yoxdursa)
            File logDirectory = new File(LOG_DIRECTORY);
            if (!logDirectory.exists()) {
                boolean created = logDirectory.mkdirs();
                if (created) {
                    System.out.println("📁 Log qovluğu yaradıldı: " + logDirectory.getAbsolutePath());
                } else {
                    System.err.println("⚠️ Log qovluğu yaradıla bilmədi, cari qovluqda saxlanacaq");
                    logDirectory = new File(".");  // Fallback to current directory
                }
            }

            // Log faylının yolunu qovluq daxilində müəyyən et
            File logFile = new File(logDirectory, "sitemap_check_" + timestamp + ".txt");
            logWriter = new PrintWriter(new FileWriter(logFile), true);

            File csvFile = new File(logDirectory, "sitemap_check_" + timestamp + ".csv");
            csvWriter = new PrintWriter(new FileWriter(csvFile), true);
            csvWriter.println("Status,URL,Encoded URL,Error Message");

            synchronized (System.out) {
                System.out.println("📁 Log faylları yaradıldı:");
                System.out.println("   📄 " + logFile.getAbsolutePath());
                System.out.println("   📊 " + csvFile.getAbsolutePath());
                System.out.println();
            }

        } catch (IOException e) {
            System.err.println("❌ Log faylları yaradıla bilmədi: " + e.getMessage());
        }
    }

    private synchronized void logToFile(String message) {
        if (logWriter != null) {
            logWriter.println(message);
        }
    }

    private synchronized void logToCsv(int statusCode, String url, String encodedUrl, String errorMsg) {
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
            synchronized (System.out) {
                System.out.println("\n✅ Log faylları saxlanıldı.");
            }
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
            synchronized (System.out) {
                System.out.println(msg);
            }
            logToFile(msg);
            totalSkipped.incrementAndGet();
            return;
        }

        try {
            String indent = "  ".repeat(depth);
            String message = indent + "📄 Sitemap açılır: " + sitemapUrl;
            synchronized (System.out) {
                System.out.println(message);
            }
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
                synchronized (System.out) {
                    System.out. println(message);
                }
                logToFile(message);

                for (int i = 0; i < sitemapNodes.getLength(); i++) {
                    Element sitemapElement = (Element) sitemapNodes.item(i);
                    NodeList locInSitemap = sitemapElement.getElementsByTagName("loc");

                    if (locInSitemap.getLength() > 0) {
                        String subSitemapUrl = locInSitemap.item(0).getTextContent().trim();

                        if (! shouldSkipSitemap(subSitemapUrl)) {
                            message = indent + "   ↳ Alt-sitemap: " + subSitemapUrl;
                            synchronized (System.out) {
                                System.out.println(message);
                            }
                            logToFile(message);
                        }

                        checkSitemap(subSitemapUrl, depth + 1);
                    }
                }
            }

            // Səhifə linkləri - paralel yoxlama
            NodeList urlNodes = doc.getElementsByTagName("url");
            if (urlNodes.getLength() > 0) {
                message = indent + "   ✓ Tapılan səhifə sayı: " + urlNodes.getLength();
                synchronized (System.out) {
                    System.out. println(message);
                }
                logToFile(message);

                // Bütün URL-ləri paralel yoxla
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < urlNodes.getLength(); i++) {
                    Element urlElement = (Element) urlNodes.item(i);
                    NodeList locNodes = urlElement.getElementsByTagName("loc");

                    if (locNodes.getLength() > 0) {
                        String url = locNodes.item(0).getTextContent().trim();
                        final int urlDepth = depth;
                        
                        // Submit task-ı executor-a
                        Future<?> future = executor.submit(() -> checkUrl(url, urlDepth));
                        futures.add(future);
                    }
                }

                // Bu sitemap üçün bütün task-ların bitməsini gözlə
                for (Future<?> future : futures) {
                    try {
                        future.get();
                    } catch (ExecutionException e) {
                        System.err.println("❌ URL yoxlama xətası: " + e.getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
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
        totalChecked.incrementAndGet();
        String indent = "  ".repeat(depth + 1);

        int maxRetries = 2;
        int retryCount = 0;

        while (retryCount <= maxRetries) {
            boolean permitAcquired = false;
            try {
                // Rate limiting
                rateLimiter.acquire();
                permitAcquired = true;
                
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
                    totalOK.incrementAndGet();
                    String output = result + " ✅";
                    synchronized (System.out) {
                        System.out. println(output);
                    }
                    logToFile(output);
                    logToCsv(statusCode, url, encodedUrl, null);
                    allResults.add(String.format("[%d] %s", statusCode, url));
                } else {
                    totalErrors.incrementAndGet();
                    String output = result + " ⚠️";
                    synchronized (System.out) {
                        System.out.println(output);
                    }
                    logToFile(output);
                    logToCsv(statusCode, url, encodedUrl, "Non-200 status");
                    allResults.add(String.format("[%d] %s", statusCode, url));
                    errorDetails.add(String.format("[%d] %s", statusCode, url));
                }

                connection.disconnect();
                Thread.sleep(50);
                return;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                totalErrors.incrementAndGet();
                String error = String.format("%s[INTERRUPTED] %s ❌", indent, url);
                synchronized (System.out) {
                    System.out.println(error);
                }
                logToFile(error);
                logToCsv(0, url, null, "Thread interrupted");
                allResults.add(String.format("[INTERRUPTED] %s", url));
                errorDetails.add(String.format("[INTERRUPTED] %s", url));
                return;
            } catch (java.net. SocketTimeoutException e) {
                retryCount++;
                if (retryCount <= maxRetries) {
                    String retryMsg = indent + "⏱️ Timeout, yenidən cəhd " + retryCount + "/" + maxRetries + ": " + url;
                    synchronized (System.out) {
                        System.out. println(retryMsg);
                    }
                    logToFile(retryMsg);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else {
                    totalErrors.incrementAndGet();
                    String error = String.format("%s[TIMEOUT] %s ❌", indent, url);
                    synchronized (System.out) {
                        System.out.println(error);
                    }
                    logToFile(error);
                    logToCsv(0, url, null, "Timeout after " + maxRetries + " retries");
                    allResults.add(String.format("[TIMEOUT] %s", url));
                    errorDetails.add(String.format("[TIMEOUT] %s", url));
                }
            } catch (Exception e) {
                totalErrors.incrementAndGet();
                String error = String.format("%s[ERROR] %s - %s ❌", indent, url, e. getMessage());
                synchronized (System.out) {
                    System.out. println(error);
                }
                logToFile(error);
                logToCsv(0, url, null, e.getMessage());
                allResults.add(String.format("[ERROR] %s - %s", url, e.getMessage()));
                errorDetails.add(String.format("[ERROR] %s - %s", url, e. getMessage()));
                return;
            } finally {
                if (permitAcquired) {
                    rateLimiter.release();
                }
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
        summary.append("   📌 Yoxlanan link sayı: ").append(totalChecked.get()).append("\n");
        if (totalSkipped.get() > 0) {
            summary.append("   ⏭️  Skip edilən sitemap sayı: ").append(totalSkipped.get()).append("\n");
        }
        summary.append("   ✅ Uğurlu (200): ").append(totalOK.get()).append("\n");
        summary.append("   ❌ Xətalı:  ").append(totalErrors.get()).append("\n");

        double successRate = totalChecked.get() > 0 ? (totalOK.get() * 100.0 / totalChecked.get()) : 0;
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
        synchronized (System.out) {
            System.out.println(summaryText);
        }
        logToFile(summaryText);
    }
}