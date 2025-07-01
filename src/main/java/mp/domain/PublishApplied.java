package mp.domain;

import java.time.LocalDate;
import java.util.*;
import lombok.*;
import mp.infra.AbstractEvent;

//<<< DDD / Domain Event
@Data
@ToString
public class PublishApplied extends AbstractEvent {

    private UUID id;
    private UUID authorId;

    public PublishApplied(Manuscript aggregate) {

        super(aggregate);
        this.id = aggregate.getId();
        this.authorId = aggregate.getAuthorId();
    }

    public PublishApplied() {
        super();
    }
}
//>>> DDD / Domain Event
