# 🗺️ StudyLeo Sitemap Checker

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.x-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Güclü və Konfiqurasiya Edilə Bilən Sitemap Yoxlama Aləti** | **Powerful & Configurable Sitemap Checker Tool**

Bu alət, veb saytların sitemap.xml fayllarındakı bütün linkləri avtomatik yoxlayır, xətalı linkləri aşkar edir və detallı hesabatlar yaradır.

---

## 📋 İçindəkilər | Table of Contents

- [Xüsusiyyətlər | Features](#-xüsusiyyətlər--features)
- [Tələblər | Requirements](#-tələblər--requirements)
- [Quraşdırma | Installation](#-quraşdırma--installation)
- [İstifadə | Usage](#-istifadə--usage)
- [Konfiqurasiya | Configuration](#-konfiqurasiya--configuration)
- [Çıxış Formatları | Output Formats](#-çıxış-formatları--output-formats)
- [Nümunələr | Examples](#-nümunələr--examples)
- [Layihə Strukturu | Project Structure](#️-layihə-strukturu--project-structure)
- [Töhfə Vermək | Contributing](#-töhfə-vermək--contributing)

---

## ✨ Xüsusiyyətlər | Features

### 🔍 SitemapChecker - Əsas Yoxlama Aləti
- ✅ **Rekursiv Sitemap Yoxlanması** - Alt-sitemapları avtomatik tapır və yoxlayır
- ✅ **Kateqoriya Filtri** - Static, Universities, Blogs, SEO Pages və s. kateqoriyaları seçici yoxlama
- ✅ **Dil Filtri** - 19+ dil dəstəyi (en, ru, tr, az, ar, fa, de, fr, zh və s.)
- ✅ **URL Kodlaşdırması** - Xüsusi simvolları avtomatik encode edir
- ✅ **Timeout Müdiriyyəti** - Avtomatik retry mexanizmi
- ✅ **Detallı Hesabatlar** - TXT və CSV formatında çıxış
- ✅ **Real-time Progress** - Canlı yoxlama nəticələri

### 🔄 SitemapReChecker - Xəta Yenidən Yoxlama Aləti
- ✅ **Avtomatik Xəta Aşkarı** - Əvvəlki hesabatlardan xətalı linkləri avtomatik tapır
- ✅ **Üçqat Yoxlama** - Hər linki 3 dəfə yoxlayır (HEAD + GET metodları)
- ✅ **Clipboard Dəstəyi** - Kopyalanmış linkləri birbaşa yoxlaya bilir
- ✅ **İnteraktiv İnterfeys** - İstifadəçi dostu konsol interfeysi
- ✅ **Fayl Birləşdirmə** - Bir neçə hesabatı birləşdirmə imkanı

---

## 📦 Tələblər | Requirements

- **Java** 21 və ya daha yuxarı
- **Maven** 3.x (build üçün)
- **İnternet Bağlantısı** (sitemap yoxlaması üçün)

---

## 🚀 Quraşdırma | Installation

### 1. Reponu Klonlayın
```bash
git clone https://github.com/nurlanbagishli10/StudyLeoSiteMapChecker.git
cd StudyLeoSiteMapChecker
```

### 2. Maven ilə Kompilyasiya Edin
```bash
mvn clean compile
```

### 3. JAR Faylı Yaradın (İstəyə bağlı)
```bash
mvn package
```

---

## 💻 İstifadə | Usage

### SitemapChecker - Əsas Yoxlama

```bash
# Maven ilə çalışdırın
mvn exec:java -Dexec.mainClass="SitemapChecker"

# Və ya birbaşa Java ilə
java -cp target/classes SitemapChecker
```

### SitemapReChecker - Xəta Yenidən Yoxlama

```bash
# Maven ilə çalışdırın
mvn exec:java -Dexec.mainClass="SitemapReChecker"

# Və ya birbaşa Java ilə
java -cp target/classes SitemapReChecker
```

---

## ⚙️ Konfiqurasiya | Configuration

### Sitemap URL Təyini
`SitemapChecker.java` faylında əsas sitemap URL-ni dəyişdirin:

```java
private static final String SITEMAP_URL = "https://studyleo.com/sitemap.xml";
```

### Kateqoriya Filtrləri
Hansı sitemap kateqoriyalarının yoxlanacağını təyin edin:

```java
private static final boolean CHECK_STATIC = false;           // /sitemaps/static/
private static final boolean CHECK_UNIVERSITIES = true;      // /sitemaps/universities/
private static final boolean CHECK_BLOGS = false;            // /sitemaps/blogs/
private static final boolean CHECK_BLOG_TAGS = false;        // /sitemaps/blog-tags/
private static final boolean CHECK_BLOG_CATEGORIES = false;  // /sitemaps/blog-categories/
private static final boolean CHECK_SEO_PAGES = false;        // /sitemaps/seo-pages/
```

### Dil Filtrləri
Yalnız müəyyən dilləri yoxlamaq üçün:

```java
// Bütün dillər
private static final Set<String> LANGUAGE_FILTER = new HashSet<>(Arrays.asList(
    "en", "ar", "ru", "fa", "ku", "az", "kk", "tr", "bg", "id", 
    "de", "zh", "fr", "ky", "ur", "so", "tk", "uz", "sw"
));

// Yalnız İngilis və Rus dilləri
private static final Set<String> LANGUAGE_FILTER = new HashSet<>(Arrays.asList("en", "ru"));

// Bütün dillər (boş set)
private static final Set<String> LANGUAGE_FILTER = new HashSet<>();
```

---

## 📊 Çıxış Formatları | Output Formats

### TXT Hesabatı
```
██████████████████████████████████████████████████████████████████████
█  🗺️  SITEMAP CHECKER - CONFIGURABLE MODE                        █
██████████████████████████████████████████████████████████████████████

🔍 Sitemap yoxlanır: https://studyleo.com/sitemap.xml
══════════════════════════════════════════════════════════════════════

📄 Sitemap açılır: https://studyleo.com/sitemap.xml
   ✓ Alt-sitemap sayı: 114 (yoxlanacaq: 2, skip: 112)

[200] https://studyleo.com/en/universities/example ✅
[404] https://studyleo.com/en/broken-link ⚠️

═══════════════════════════════════════════════════════════════════════
📊 YEKUN NƏTİCƏ
═══════════════════════════════════════════════════════════════════════
   📌 Yoxlanan link sayı: 1500
   ⏭️  Skip edilən sitemap sayı: 112
   ✅ Uğurlu (200): 1498
   ❌ Xətalı: 2
   📈 Uğur nisbəti: 99.87%
```

### CSV Hesabatı
```csv
Status,URL,Encoded URL,Error Message
200,"https://studyleo.com/en/universities/example","https://studyleo.com/en/universities/example",""
404,"https://studyleo.com/en/broken-link","https://studyleo.com/en/broken-link","Non-200 status"
```

---

## 📁 Çıxış Faylları | Output Files

Alət avtomatik olaraq aşağıdakı faylları yaradır:

| Fayl | Təsvir |
|------|--------|
| `sitemap_check_YYYY-MM-DD_HH-mm-ss.txt` | Detallı log hesabatı |
| `sitemap_check_YYYY-MM-DD_HH-mm-ss.csv` | CSV formatında nəticələr |
| `recheck_report_TIMESTAMP.txt` | Yenidən yoxlama hesabatı |

---

## 🔧 Texniki Detallar | Technical Details

### HTTP İstək Konfiqurasiyası
- **Metod**: HEAD (ilkin yoxlama üçün sürətli)
- **Timeout**: 10 saniyə (connect & read)
- **Retry**: Timeout halında 2 dəfə yenidən cəhd
- **User-Agent**: `Mozilla/5.0 (Sitemap Checker)`
- **Rate Limiting**: Hər istək arasında 50ms gözləmə

### Dəstəklənən Status Kodları
| Kod | Əməliyyat |
|-----|-----------|
| 200 | ✅ Uğurlu |
| 3xx | 🔄 Yönləndirmə (redirect) |
| 4xx | ⚠️ Klient xətası |
| 5xx | ❌ Server xətası |

---

## 📖 Nümunələr | Examples

### Yalnız Universitet Səhifələrini Yoxlamaq

```java
private static final boolean CHECK_STATIC = false;
private static final boolean CHECK_UNIVERSITIES = true;  // ✅
private static final boolean CHECK_BLOGS = false;
private static final boolean CHECK_BLOG_TAGS = false;
private static final boolean CHECK_BLOG_CATEGORIES = false;
private static final boolean CHECK_SEO_PAGES = false;
```

### Yalnız Türk və Azərbaycan Dillərini Yoxlamaq

```java
private static final Set<String> LANGUAGE_FILTER = new HashSet<>(Arrays.asList("tr", "az"));
```

### Bütün Kateqoriyaları, Bütün Dillərdə Yoxlamaq

```java
private static final boolean CHECK_STATIC = true;
private static final boolean CHECK_UNIVERSITIES = true;
private static final boolean CHECK_BLOGS = true;
private static final boolean CHECK_BLOG_TAGS = true;
private static final boolean CHECK_BLOG_CATEGORIES = true;
private static final boolean CHECK_SEO_PAGES = true;

private static final Set<String> LANGUAGE_FILTER = new HashSet<>(); // Boş = bütün dillər
```

---

## 🗂️ Layihə Strukturu | Project Structure

```
StudyLeoSiteMapChecker/
├── src/
│   └── main/
│       └── java/
│           ├── SitemapChecker.java      # Əsas sitemap yoxlama aləti
│           ├── SitemapReChecker.java    # Xəta yenidən yoxlama aləti
│           └── org/example/
│               └── Main.java            # Default entry point
├── pom.xml                              # Maven konfiqurasiyası
├── .gitignore
└── README.md
```

---

## 🤝 Töhfə Vermək | Contributing

Töhfələriniz xoş qarşılanır! Zəhmət olmasa:

1. Layihəni fork edin
2. Yeni branch yaradın (`git checkout -b feature/YeniXüsusiyyət`)
3. Dəyişikliklərinizi commit edin (`git commit -m 'Yeni xüsusiyyət əlavə edildi'`)
4. Branch-a push edin (`git push origin feature/YeniXüsusiyyət`)
5. Pull Request açın

---

## 📝 Lisenziya | License

Bu layihə MIT Lisenziyası altında lisenziyalaşdırılıb. Ətraflı məlumat üçün `LICENSE` faylına baxın.

---

## 👨‍💻 Müəllif | Author

**Nurlan Bağışlı**

- GitHub: [@nurlanbagishli10](https://github.com/nurlanbagishli10)

---

## 🙏 Təşəkkürlər | Acknowledgments

- [StudyLeo](https://studyleo.com) - Test məlumatları üçün
- Java XML Parser API
- Maven Build Tool

---

<p align="center">
  <b>⭐ Bu layihə sizə kömək etdisə, ulduz verməyi unutmayın!</b>
</p>
