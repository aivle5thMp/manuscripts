package mp.domain;

import java.util.*;
import lombok.*;
import mp.domain.*;
import mp.infra.AbstractEvent;

@Data
@ToString
public class BookPublished extends AbstractEvent {

    private UUID id;
    private String authorId;
    private String authorName;
    private String title;
    private String category;
    private String content;
    private String summary;
    private String audioUrl;
    private String imageUrl;
    private String todayCount;
    private String totalCount;
}
