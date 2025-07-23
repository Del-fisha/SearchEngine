package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.IndexingIsAlreadyRunningException;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.time.LocalDateTime;


@Service
public class IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    @Getter
    private boolean isIndexingRunning;

    @Autowired
    public IndexingService(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
    }

    public synchronized void startIndexing() {
        if (isIndexingRunning) {
            throw new IndexingIsAlreadyRunningException();
        }
        isIndexingRunning = true;
        try {
            initializeIndexing();
            indexSites();
        } finally {
            isIndexingRunning = false;
        }
    }


    private synchronized void indexSites() {
        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatusTime(LocalDateTime.now());
            siteEntity.setStatus(Status.INDEXING);
            siteRepository.save(siteEntity);
        }
    }

    private synchronized void initializeIndexing() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
    }

}
