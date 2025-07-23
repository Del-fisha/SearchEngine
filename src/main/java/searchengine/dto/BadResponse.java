package searchengine.dto;


public class BadResponse extends Response {
    private String error;

    public BadResponse(String error) {
        this.result = false;
        this.error = error;
    }
}
