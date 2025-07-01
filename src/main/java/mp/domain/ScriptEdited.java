package mp.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import mp.domain.*;
import mp.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class ScriptEdited extends AbstractEvent {

    private UUID id;
    private UUID authorId;
    private String content;
    private String title;
    private Status status;

    public ScriptEdited(Manuscript aggregate) {
        super(aggregate);
        this.id = aggregate.getId();
        this.authorId = aggregate.getAuthorId();
        this.title = aggregate.getTitle();
        this.content = aggregate.getContent();
        this.status = aggregate.getStatus();
    }

    public ScriptEdited() {
        super();
    }
}
//>>> DDD / Domain Event
