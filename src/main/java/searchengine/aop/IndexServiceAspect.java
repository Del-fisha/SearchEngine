package searchengine.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class IndexServiceAspect {
    Logger logger = LoggerFactory.getLogger(IndexServiceAspect.class);

    @Before("execution(* searchengine.services.IndexingService.*initializeIndexing(..))")
    public void logBeforeInitializeIndexing() {
        logger.info("Запуск индексации");
    }

    @AfterReturning("execution(* searchengine.services.IndexingService.*initializeIndexing(..))")
    public void logAfterInitializeIndexing() {
        logger.info("Данные очищены");
    }

    @Before("execution(* searchengine.services.IndexingService.*indexSites(..))")
    public void logBeforeIndexSites() {
        logger.info("Обход по sites");
    }

    @Before("execution(* searchengine.services.IndexingService.*indexSites(..)) && args()")
    public void logEachSite(JoinPoint joinPoint) {
        Object target = joinPoint.getTarget();
    }

    @AfterReturning("execution(* searchengine.services.IndexingService.*indexSites(..))")
    public void logAfterIndexSites() {
        logger.info("Обход по sites завершён");
        
    }

    @AfterReturning("execution(* searchengine.services.IndexingService.startIndexing(..))")
    public void logIndexingSuccess() {
        logger.info("Индексация прошла успешно");
    }

    @After("execution(* searchengine.services.IndexingService.startIndexing(..))")
    public void logIndexingFinished() {
        logger.info("Индексация ВСЕХ сайтов завершена (базовый этап).");
    }

}
