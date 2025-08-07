package searchengine.services;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.exception.IndexingIsAlreadyRunningException;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.SiteEntity;
import searchengine.model.Status;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;

@Service
public class IndexingService {

    private final SitesList sitesList;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    @Getter
    private boolean isIndexingRunning;
    private final ForkJoinPool forkJoinPool;

    @Autowired
    public IndexingService(SitesList sitesList, SiteRepository siteRepository, PageRepository pageRepository) {
        this.sitesList = sitesList;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.forkJoinPool = new ForkJoinPool();
    }

    public synchronized void startIndexing() {
        if (isIndexingRunning) {
            throw new IndexingIsAlreadyRunningException();
        }
        isIndexingRunning = true;
        try {
            // initializeIndexing(); // если нужно чистить
            indexSites();
        } finally {
            isIndexingRunning = false;
        }
    }

    /**
     * Запускает параллельную индексацию всех сайтов.
     * Сообщение для каждого сайта появляется после его завершения.
     * Итоговое сообщение — после завершения всех задач.
     */
    private synchronized void indexSites() {
        List<ForkJoinTask<Void>> tasks = new ArrayList<>();
        List<SiteEntity> siteEntities = new ArrayList<>();

        // 1. Для каждого сайта — создать сущность и задачу, сразу submit-ить её в пул
        for (Site site : sitesList.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatusTime(LocalDateTime.now());
            siteEntity.setStatus(Status.INDEXING);
            SiteEntity savedEntity = siteRepository.save(siteEntity);
            siteEntities.add(savedEntity);

            PageCrawler crawler = new PageCrawler(savedEntity, siteRepository, pageRepository, savedEntity.getUrl());


            ForkJoinTask<Void> task = forkJoinPool.submit(crawler);
            tasks.add(task);

//            siteEntity.setLastError(e.getMessage());
//            siteEntity.setStatus(Status.FAILED);
//            siteEntity.setStatusTime(LocalDateTime.now());
//            System.out.println(savedEntity.getName() + " Какая-то ошибка!");
        }

        // 2. Теперь ждём завершения всех задач, по одной, и пишем сообщение для каждой
        for (int i = 0; i < tasks.size(); i++) {
            tasks.get(i).join();
            System.out.println(siteEntities.get(i).getUrl() + " : Индексация выполнена");
        }

        System.out.println("Программа выполнена");
    }

    // Если нужно обнуление перед началом:
    private synchronized void initializeIndexing() {
        siteRepository.deleteAll();
        pageRepository.deleteAll();
    }

    // Корректная остановка пула перед завершением программы/бина
    @PreDestroy
    public void stopIndexing() {
        forkJoinPool.shutdown();
        try {
            forkJoinPool.awaitTermination(100000L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
