package searchengine.services;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveAction;

public class PageCrawler extends RecursiveAction {

    @Getter
    private final SiteEntity site;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;
    private static final Object lock = new Object();

    static Map<String, Boolean> alreadySavedPages = new ConcurrentHashMap<>();
    private Set<String> currentPageSet = new HashSet<>();
    private final String currentUrl;

    public PageCrawler(SiteEntity site, SiteRepository siteRepo, PageRepository pageRepo, String currentUrl) {
        this.site = site;
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
        this.currentUrl = currentUrl;
    }

    public void crawlPage(String url) {
        Connection.Response response = null;
        Document doc = null;
        int statusCode = 0;

        try {
            response = Jsoup.connect(url).execute();
            statusCode = response.statusCode();
            doc = response.parse();
        } catch (IOException e) {
            site.setLastError(e.getMessage());
            throw new RuntimeException(e);
        }

        if (!alreadySavedPages.containsKey(url)) {
            PageEntity pageEntity = createPageEntity(url, statusCode, doc);
            saveCurrentPageEntity(pageEntity);
            addAllLinksToCurrentSet(pageEntity);
        }
    }

    private void saveCurrentPageEntity(PageEntity pageEntity) {
        alreadySavedPages.put(pageEntity.getPath(), true);
        pageRepo.save(pageEntity);
    }

    private PageEntity createPageEntity(String url, int statusCode, Document doc) {

        PageEntity currentPage = new PageEntity();
        currentPage.setSite(site);

        currentPage.setPath(url);
        currentPage.setCode(statusCode);
        currentPage.setContent(doc.outerHtml());

        return currentPage;
    }

    private Set<String> getLinks(PageEntity pageEntity) {
        Set<String> links = new HashSet<>();

        String pageHtml = pageEntity.getContent();
        Document doc = Jsoup.parse(pageHtml, pageEntity.getPath());
        Elements linkElements = doc.select("a[href]");
        for (Element linkElement : linkElements) {
            if (isValidLink(linkElement)) {
                String href = linkElement.attr("abs:href");

                if (href.endsWith("/") || href.endsWith("#")) {
                    href = href.substring(0, href.length() - 1);
                }
                if (!alreadySavedPages.containsKey(href)) {
                    links.add(href);
                }

            }
        }

        return links;
    }

    private void addAllLinksToCurrentSet(PageEntity pageEntity) {
        Set<String> links = getLinks(pageEntity);
        currentPageSet.addAll(links);
    }

    private boolean isValidLink(Element link) {
        String href = link.attr("abs:href").trim();
        String siteUrl = site.getUrl().trim();

        if (!href.endsWith("html")) {
            return false;
        }

        return href.startsWith(siteUrl);
    }

    @Override
    protected void compute() {
        System.out.println("Нахожусь в " + currentUrl);
        if (alreadySavedPages.containsKey(currentUrl)) {
            System.out.println("--------------- Проверка на уникальность не пройдена --------------------------");
            return; // ToDo Подумать где оставить логику проверки на наличие в alreadySavedPages
                    // (Эта логика ещё используется в saveCurrentPageEntity)
        }
        System.out.println("************* Проверка на уникальность пройдена **************");
        crawlPage(currentUrl);
        alreadySavedPages.put(currentUrl, true);
        List<PageCrawler> pageCrawlers = new ArrayList<>();
        for (String page : currentPageSet) {
            PageCrawler newCrawler = new PageCrawler(site, siteRepo, pageRepo, page);
            pageCrawlers.add(newCrawler);
        }

        invokeAll(pageCrawlers);
    }

    private void printLinks() {
        synchronized (lock) {
            System.out.println("Вот ссылки страницы" + currentUrl);
            currentPageSet.forEach(System.out::println);
        }
    }
}
