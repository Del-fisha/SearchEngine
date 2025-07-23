package searchengine.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import searchengine.model.SiteEntity;

@Aspect
@Component
public class SiteRepositoryAspect {
    Logger logger = LoggerFactory.getLogger(SiteRepositoryAspect.class);

    @AfterReturning(value = "execution(* searchengine.repository.SiteRepository.save(..))", returning = "result")
    public void afterSiteSave(JoinPoint joinPoint, Object result) {
        if (result instanceof SiteEntity entity) {
            logger.info("Site: {} сохранён", entity.getName());
        }
    }

}
