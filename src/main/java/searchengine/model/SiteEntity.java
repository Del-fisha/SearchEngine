package searchengine.model;

import lombok.Data;
import org.springframework.stereotype.Component;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Data
@Component
public class SiteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, unique = true)
    private int id;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "status_time", nullable = false)
    private LocalDateTime statusTime;

    @Column(name = "url", nullable = false, unique = true)
    private String url;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}
