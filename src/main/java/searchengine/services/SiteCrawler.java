package searchengine.services;

import lombok.Getter;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SiteCrawler {

    @Getter
    private final SiteEntity site;
    private final SiteRepository siteRepo;
    private final PageRepository pageRepo;


    static Map<String, Boolean> allPages = new ConcurrentHashMap<>();
    private Set<String> pageSet = new HashSet<>();

    public SiteCrawler(final SiteEntity site, final SiteRepository siteRepo, final PageRepository pageRepo) {
        this.site = site;
        this.siteRepo = siteRepo;
        this.pageRepo = pageRepo;
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

        PageEntity pageEntity = createPageEntity(url, statusCode, doc);
        saveCurrentPageEntity(pageEntity);
        Set<String> links = getLinks(pageEntity);
        pageSet.addAll(links);
    }

    private void saveCurrentPageEntity(PageEntity pageEntity) {
        if (!allPages.containsKey(pageEntity.getPath())) {
            allPages.put(pageEntity.getPath(), true);
            pageRepo.save(pageEntity);
        }
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
                links.add(href);
                if (!allPages.containsKey(href)) {
                    allPages.put(href, true);
                }
            }
        }

        return links;
    }


    private boolean isValidLink(Element link) {
        String href = link.attr("abs:href").trim();
        String siteUrl = site.getUrl().trim();

        if (href.isEmpty()) {
            return false;
        }

        return href.startsWith(siteUrl);
    }


}
