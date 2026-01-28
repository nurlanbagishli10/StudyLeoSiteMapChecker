import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.io.*;
import java.util.regex.*;

public class SitemapReChecker {

    private int totalRechecked = 0;
    private int fixedErrors = 0;
    private int stillErrors = 0;
    private List<RecheckResult> recheckResults = new ArrayList<>();

    public static void main(String[] args) {
        SitemapReChecker reChecker = new SitemapReChecker();
        reChecker.run();
    }

    public void run() {
        printHeader();

        // Avtomatik xətalı linkləri tap
        List<String> errorUrls = autoDetectErrorUrls();

        if (errorUrls.isEmpty()) {
            System.out.println("❌ Heç bir xətalı link tapılmadı!");
            System.out.println("\n💡 Xətalı linkləri əl ilə daxil etmək istəyirsiniz?  (y/n): ");

            Scanner scanner = new Scanner(System.in);
            String answer = scanner.nextLine().trim().toLowerCase();

            if (answer.equals("y") || answer.equals("yes") || answer.equals("bəli")) {
                errorUrls = readErrorUrlsFromUser();
            } else {
                return;
            }
        }

        if (errorUrls.isEmpty()) {
            System.out.println("❌ Yoxlanacaq link yoxdur!");
            return;
        }

        System.out.println("📋 Tapılan xətalı link sayı: " + errorUrls.size());
        System.out.println("🔄 Yenidən yoxlama başlayır...\n");
        System.out.println("═".repeat(80) + "\n");

        // Hər bir linki 3 dəfə yoxla
        for (String errorUrl :  errorUrls) {
            recheckUrl(errorUrl);
        }

        printSummary();
        offerExport();
    }

    /**
     * ⭐ ƏSAS FUNKSIYA:  Avtomatik olaraq xətalı linkləri tap
     * Axtarış sırası:
     * 1. Clipboard (əgər copy etmisənsə)
     * 2. sitemap_errors.txt
     * 3. sitemap_report.txt
     * 4. sitemap_log.txt
     * 5. Cari direktoriyada *. txt fayllar
     */
    // autoDetectErrorUrls() metodunu dəyişdir:

    private List<String> autoDetectErrorUrls() {
        List<String> errorUrls = new ArrayList<>();

        System.out.println("🔍 Xətalı linklər axtarılır.. .\n");

        // 1. Clipboard-dan yoxla
        errorUrls = tryReadFromClipboard();
        if (!errorUrls.isEmpty()) {
            System.out.println("✅ Clipboard-dan " + errorUrls.size() + " xətalı link tapıldı!");
            return errorUrls;
        }

        // 2. ⭐ ƏN SON yaradılan fayla prioritet ver
        File latestFile = findLatestSitemapFile();

        if (latestFile != null) {
            System.out.println("📂 Ən son fayl tapıldı: " + latestFile.getName());
            System.out.println("   📅 Tarix: " + new Date(latestFile.lastModified()));
            errorUrls = readErrorUrlsFromFile(latestFile. getName());

            if (!errorUrls.isEmpty()) {
                System.out. println("✅ " + latestFile.getName() + " faylından " + errorUrls. size() + " xətalı link tapıldı!\n");
                return errorUrls;
            }
        }

        System.out.println("⚠️ Avtomatik heç bir xətalı link tapılmadı.\n");
        return errorUrls;
    }

    /**
     * ⭐ YENİ METOD: Ən son sitemap faylını tap
     */
    private File findLatestSitemapFile() {
        File dir = new File(".");
        File[] files = dir. listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if (! file.isFile()) return false;

                String name = file.getName().toLowerCase();

                // Sitemap ilə əlaqəli faylları götür
                return (name.contains("sitemap") ||
                        name.contains("error") ||
                        name. contains("report") ||
                        name.contains("log")) &&
                        (name.endsWith(".txt") || name.endsWith(".csv"));
            }
        });

        if (files == null || files.length == 0) {
            return null;
        }

        // Tarixə görə sırala (ən yeni ən başda)
        Arrays.sort(files, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        // Bütün tapılan faylları göstər
        System.out.println("📁 Tapılan fayllar (ən yenidən köhnəyə):");
        for (int i = 0; i < files.length && i < 5; i++) {
            File f = files[i];
            String date = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .format(new Date(f. lastModified()));
            System.out.println("   " + (i+1) + ". " + f.getName() + " (" + date + ")");
        }
        System.out.println();

        // İstifadəçiyə seçim imkanı ver
        return selectFileInteractively(files);
    }

    /**
     * ⭐ YENİ METOD: İstifadəçiyə fayl seçimi təklif et
     */
    private File selectFileInteractively(File[] files) {
        if (files.length == 1) {
            return files[0]; // Tək fayl varsa, onu götür
        }

        Scanner scanner = new Scanner(System.in);
        System.out.println("📌 Hansı faylı yoxlamaq istəyirsiniz? ");
        System.out.println("   1. Ən son faylı avtomatik seç (tövsiyə olunur)");
        System.out.println("   2. Özüm seçim edəcəm");
        System.out.println("   3. Bütün faylları yoxla");
        System.out.print("\nSeçim (1/2/3) [default: 1]: ");

        String choice = scanner.nextLine().trim();

        if (choice.isEmpty() || choice.equals("1")) {
            // Ən son faylı seç
            return files[0];
        }
        else if (choice.equals("2")) {
            // Əl ilə seçim
            System.out.print("\nFayl nömrəsini daxil edin (1-" + files.length + "): ");
            try {
                int fileIndex = Integer.parseInt(scanner. nextLine().trim()) - 1;
                if (fileIndex >= 0 && fileIndex < files.length) {
                    return files[fileIndex];
                }
            } catch (NumberFormatException e) {
                System.out.println("❌ Yanlış nömrə, ən son fayl seçildi.");
            }
            return files[0];
        }
        else if (choice.equals("3")) {
            // Bütün faylları birləşdir
            return mergeAllFiles(files);
        }

        return files[0]; // Default:  ən son
    }

    /**
     * ⭐ YENİ METOD: Bütün faylları birləşdir
     */
    private File mergeAllFiles(File[] files) {
        try {
            String mergedFilename = "merged_errors_" + System.currentTimeMillis() + ".txt";
            PrintWriter writer = new PrintWriter(new FileWriter(mergedFilename));

            Set<String> allUrls = new LinkedHashSet<>(); // Dublikatları çıxart

            for (File file : files) {
                List<String> urls = readErrorUrlsFromFile(file.getName());
                allUrls.addAll(urls);
            }

            for (String url : allUrls) {
                writer.println(url);
            }

            writer.close();

            System.out.println("✅ " + files.length + " fayl birləşdirildi → " + mergedFilename);
            System.out.println("   Toplam unikal xətalı link: " + allUrls.size() + "\n");

            return new File(mergedFilename);

        } catch (IOException e) {
            System.err.println("❌ Faylları birləşdirmə xətası: " + e.getMessage());
            return files[0]; // Xəta olarsa, ən son faylı qaytar
        }
    }

    /**
     * Clipboard-dan oxumağa çalış (Windows/Mac/Linux)
     */
    private List<String> tryReadFromClipboard() {
        List<String> urls = new ArrayList<>();

        try {
            // Java AWT clipboard
            java.awt.datatransfer. Clipboard clipboard =
                    java.awt. Toolkit.getDefaultToolkit().getSystemClipboard();

            if (clipboard.isDataFlavorAvailable(java.awt.datatransfer.DataFlavor.stringFlavor)) {
                String clipboardText = (String) clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor);

                // Clipboard-da xətalı linklər varmı?
                String[] lines = clipboardText.split("\n");
                for (String line : lines) {
                    String url = extractErrorUrl(line);
                    if (url != null && !urls.contains(url)) {
                        urls.add(url);
                    }
                }
            }
        } catch (Exception e) {
            // Clipboard oxuna bilməzsə, sessiz keç
        }

        return urls;
    }

    /**
     * Fayldan xətalı linkləri oxu
     */
    private List<String> readErrorUrlsFromFile(String filename) {
        List<String> urls = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String url = extractErrorUrl(line);

                if (url != null && !urls.contains(url)) {
                    urls.add(url);
                }
            }

        } catch (FileNotFoundException e) {
            // Fayl yoxdursa, sessiz keç
        } catch (IOException e) {
            System.err.println("⚠️ Fayl oxuma xətası (" + filename + "): " + e.getMessage());
        }

        return urls;
    }

    /**
     * ⭐ ÇOX VACIB: Sətirdən xətalı URL çıxart
     * Dəstəklənən formatlar:
     * - [404] https://example.com ⚠️
     * - [ERROR] https://example.com - Connection timeout ❌
     * - ❌ XƏTALI LINKLƏR:  [404] https://example.com
     * - https://example.com (200 olmayanlar)
     */
    private String extractErrorUrl(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        line = line.trim();

        // Yalnız xətalı linkləri götür (200 olanları keç)
        if (line.contains("[200]") || line.contains("✅")) {
            return null;
        }

        // Xəta göstəricilərini yoxla
        boolean isError = line.contains("[404]") ||
                line.contains("[ERROR]") ||
                line.contains("[500]") ||
                line. contains("[403]") ||
                line. contains("[401]") ||
                line.contains("⚠️") ||
                line.contains("❌") ||
                line.matches(".*\\[\\d{3}\\].*"); // [XXX] formatı

        if (! isError && ! line.startsWith("http")) {
            return null;
        }

        // URL-i tap
        Pattern urlPattern = Pattern.compile("(https?://[^\\s⚠️❌✅]+)");
        Matcher matcher = urlPattern.matcher(line);

        if (matcher.find()) {
            String url = matcher.group(1);
            // Sondakı nöqtə, vergül və s.  sil
            url = url.replaceAll("[,;. \\)\\]]+$", "");
            return url;
        }

        return null;
    }

    /**
     * İstifadəçidən əl ilə link daxil etməsini istə
     */
    private List<String> readErrorUrlsFromUser() {
        List<String> urls = new ArrayList<>();
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n📝 Xətalı linkləri daxil edin (boş sətr göndərməklə bitirin):");
        System.out. println("   Format: [404] https://example.com");
        System.out.println("   və ya sadəcə: https://example.com\n");

        while (true) {
            System.out.print("Link:  ");
            String line = scanner.nextLine().trim();

            if (line.isEmpty()) {
                break;
            }

            String url = extractErrorUrl(line);
            if (url == null) {
                // Sadəcə URL-dirsə
                if (line.startsWith("http")) {
                    url = line;
                }
            }

            if (url != null && !urls.contains(url)) {
                urls.add(url);
                System.out.println("   ✅ Əlavə edildi");
            }
        }

        return urls;
    }

    private void recheckUrl(String url) {
        totalRechecked++;

        System.out.println("🔍 Yoxlanır: " + url);

        RecheckResult result = new RecheckResult(url);
        int[] statusCodes = new int[3];
        long[] responseTimes = new long[3];

        // 3 dəfə yoxla
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                System.out.print("   Cəhd " + attempt + "/3: ");

                long startTime = System.currentTimeMillis();
                int statusCode = checkSingleUrl(url, attempt);
                long endTime = System.currentTimeMillis();

                statusCodes[attempt - 1] = statusCode;
                responseTimes[attempt - 1] = endTime - startTime;

                if (statusCode == 200) {
                    System.out.println("✅ OK (" + responseTimes[attempt - 1] + "ms)");
                } else if (statusCode >= 300 && statusCode < 400) {
                    System.out. println("🔄 REDIRECT [" + statusCode + "] (" + responseTimes[attempt - 1] + "ms)");
                } else {
                    System.out.println("⚠️ [" + statusCode + "] (" + responseTimes[attempt - 1] + "ms)");
                }

                if (attempt < 3) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                statusCodes[attempt - 1] = -1;
                System.out.println("❌ XƏTA:  " + e.getMessage());

                try {
                    if (attempt < 3) Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        result.setAttempts(statusCodes, responseTimes);
        recheckResults.add(result);

        // Nəticəni qiymətləndir
        boolean hasSuccess = false;
        for (int code : statusCodes) {
            if (code == 200 || (code >= 300 && code < 400)) {
                hasSuccess = true;
                break;
            }
        }

        if (hasSuccess) {
            fixedErrors++;
            System.out.println("   ✅ NƏTİCƏ:  Link işləkdir\n");
        } else {
            stillErrors++;
            System. out.println("   ❌ NƏTİCƏ: Link hələ də problemlidir\n");
        }

        System.out.println("─".repeat(80));
    }

    private int checkSingleUrl(String url, int attemptNumber) throws Exception {
        String encodedUrl = encodeUrl(url);

        HttpURLConnection connection = (HttpURLConnection) new URL(encodedUrl).openConnection();

        if (attemptNumber == 1) {
            connection.setRequestMethod("HEAD");
        } else {
            connection.setRequestMethod("GET");
        }

        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);

        connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        connection.setRequestProperty("Connection", "keep-alive");

        int statusCode = connection.getResponseCode();

        if (statusCode >= 300 && statusCode < 400) {
            String redirectUrl = connection.getHeaderField("Location");
            System.out.print(" 🔄 → " + redirectUrl + " ");
        }

        connection.disconnect();
        return statusCode;
    }

    private String encodeUrl(String url) throws Exception {
        URL urlObj = new URL(url);
        String path = urlObj.getPath();
        String[] parts = path.split("/");
        StringBuilder encodedPath = new StringBuilder();

        for (String part : parts) {
            if (! part.isEmpty()) {
                String encoded = java.net.URLEncoder.encode(part, "UTF-8");
                encoded = encoded.replace("+", "%20");
                encodedPath.append("/").append(encoded);
            }
        }

        String query = urlObj.getQuery();
        String queryPart = (query != null) ? "?" + query : "";

        return urlObj.getProtocol() + "://" + urlObj.getHost() + encodedPath. toString() + queryPart;
    }

    private void printHeader() {
        System.out. println("\n" + "█".repeat(80));
        System.out.println("█" + " ". repeat(78) + "█");
        System.out.println("█  🔄  SITEMAP RE-CHECKER - Xətalı linklərin avtomatik yoxlanması        █");
        System.out. println("█" + " ".repeat(78) + "█");
        System.out.println("█". repeat(80) + "\n");
    }

    private void printSummary() {
        System.out.println("\n" + "═".repeat(80));
        System.out.println("📊 YENIDƏN YOXLAMA NƏTİCƏSİ");
        System.out.println("═".repeat(80));
        System.out.println("   📌 Yenidən yoxlanan link sayı: " + totalRechecked);
        System.out.println("   ✅ Düzələn/İşlək linklər: " + fixedErrors);
        System.out.println("   ❌ Hələ də problemli:  " + stillErrors);

        double fixRate = totalRechecked > 0 ?  (fixedErrors * 100.0 / totalRechecked) : 0;
        System.out.println("   📈 Uğur nisbəti: " + String.format("%.2f", fixRate) + "%");

        System.out.println("\n" + "─".repeat(80));
        System.out.println("📋 DETALLI NƏTICƏLƏR:");
        System.out.println("─".repeat(80));

        for (RecheckResult result : recheckResults) {
            System.out.println("\n🔗 " + result.url);
            System.out.println("   Cəhd 1 (HEAD): [" + formatStatus(result.statusCodes[0]) + "] - " + result.responseTimes[0] + "ms");
            System.out.println("   Cəhd 2 (GET):  [" + formatStatus(result.statusCodes[1]) + "] - " + result.responseTimes[1] + "ms");
            System.out.println("   Cəhd 3 (GET):  [" + formatStatus(result.statusCodes[2]) + "] - " + result. responseTimes[2] + "ms");

            String verdict = result.hasSuccess() ? "✅ İŞLƏK" : "❌ PROBLEMLİ";
            System.out. println("   Qərar: " + verdict);
        }

        System.out.println("\n" + "█".repeat(80));
        System.out.println("█  ✅ YENIDƏN YOXLAMA TAMAMLANDI!                                           █");
        System.out. println("█". repeat(80) + "\n");
    }

    private String formatStatus(int code) {
        if (code == -1) return "ERROR";
        return String.valueOf(code);
    }

    private void offerExport() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("📄 Nəticələri fayla export etmək istəyirsiniz? (y/n): ");
        String answer = scanner.nextLine().trim().toLowerCase();

        if (answer.equals("y") || answer.equals("yes") || answer.equals("bəli")) {
            exportToFile();
        }
    }

    private void exportToFile() {
        String filename = "recheck_report_" + System.currentTimeMillis() + ".txt";

        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("SITEMAP RE-CHECK REPORT");
            writer.println("Generated: " + new Date());
            writer.println("=". repeat(80));
            writer.println();

            writer.println("SUMMARY:");
            writer.println("Total Rechecked: " + totalRechecked);
            writer.println("Fixed/Working: " + fixedErrors);
            writer.println("Still Broken: " + stillErrors);
            writer.println();

            writer.println("DETAILED RESULTS:");
            writer.println("=".repeat(80));

            for (RecheckResult result : recheckResults) {
                writer. println();
                writer.println("URL:  " + result.url);
                writer.println("  Attempt 1 (HEAD): [" + formatStatus(result.statusCodes[0]) + "] - " + result.responseTimes[0] + "ms");
                writer.println("  Attempt 2 (GET):  [" + formatStatus(result.statusCodes[1]) + "] - " + result.responseTimes[1] + "ms");
                writer.println("  Attempt 3 (GET):  [" + formatStatus(result.statusCodes[2]) + "] - " + result.responseTimes[2] + "ms");
                writer.println("  Status:  " + (result.hasSuccess() ? "WORKING" : "BROKEN"));
            }

            System.out.println("✅ Report faylı yaradıldı:  " + filename);

        } catch (IOException e) {
            System.err.println("❌ Fayl yazma xətası:  " + e.getMessage());
        }
    }

    static class RecheckResult {
        String url;
        int[] statusCodes = new int[3];
        long[] responseTimes = new long[3];

        RecheckResult(String url) {
            this.url = url;
        }

        void setAttempts(int[] codes, long[] times) {
            this.statusCodes = codes;
            this.responseTimes = times;
        }

        boolean hasSuccess() {
            for (int code : statusCodes) {
                if (code == 200 || (code >= 300 && code < 400)) {
                    return true;
                }
            }
            return false;
        }
    }
}