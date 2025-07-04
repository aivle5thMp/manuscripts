package mp.domain;

import java.util.UUID;
import javax.persistence.*;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import mp.ManuscriptsApplication;

@Entity
@Table(name = "Manuscript_table")
@Data
//<<< DDD / Aggregate Root
public class Manuscript {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Type(type = "uuid-char")
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Type(type = "uuid-char")
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID userId;

    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)            // (권장) enum 을 문자열로 저장
    @Column(nullable = false)
    private Status status = Status.DRAFT;

    public enum Status {
        DRAFT, SUBMITTED, UNDER_REVIEW, PUBLISHED, REJECTED
    }


    public static ManuscriptRepository repository() {
        ManuscriptRepository manuscriptRepository = ManuscriptsApplication.applicationContext.getBean(
            ManuscriptRepository.class
        );
        return manuscriptRepository;
    }
}
//>>> DDD / Aggregate Root
