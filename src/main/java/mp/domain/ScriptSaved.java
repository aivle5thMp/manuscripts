package mp.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import mp.domain.*;
import mp.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class ScriptSaved extends AbstractEvent {

//    id == 원고 ID
    private UUID id;
    private UUID authorId;
    private String title;
    private String content;
    private Status status;

    public ScriptSaved(Manuscript aggregate) {
        super(aggregate);
        this.id = aggregate.getId();
        this.authorId = aggregate.getAuthorId();
        this.title = aggregate.getTitle();
        this.content = aggregate.getContent();
        this.status = aggregate.getStatus();
    }

    public ScriptSaved() {
        super();
    }
}
//>>> DDD / Domain Event
