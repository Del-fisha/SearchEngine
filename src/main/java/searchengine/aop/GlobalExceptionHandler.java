package searchengine.aop;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.IndexingIsAlreadyRunningException;
import searchengine.dto.BadResponse;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IndexingIsAlreadyRunningException.class)
    public ResponseEntity<BadResponse> indexingIsAlreadyRunning(IndexingIsAlreadyRunningException ex) {
        BadResponse badResponse = new BadResponse(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(badResponse);
    }
}
