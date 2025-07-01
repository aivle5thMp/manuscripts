package mp.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import mp.domain.*;
import mp.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class ScriptDeleted extends AbstractEvent {

    private UUID id;
    private UUID authorId;

    public ScriptDeleted(Manuscript aggregate) {
        super(aggregate);
        this.id = aggregate.getId();
        this.authorId = aggregate.getAuthorId();
    }

    public ScriptDeleted() {
        super();
    }
}
//>>> DDD / Domain Event
