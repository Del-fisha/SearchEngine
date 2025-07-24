package searchengine.exception;

public class IndexingIsAlreadyRunningException extends RuntimeException {
    public IndexingIsAlreadyRunningException() {
        super("Индексация уже запущена");
    }
}
