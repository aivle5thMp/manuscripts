package mp.infra.dto;

import mp.domain.Manuscript;
import com.fasterxml.jackson.annotation.JsonProperty;
import mp.domain.Status;

import java.util.UUID;

public class MyDto {
    @JsonProperty("script_id")
    private UUID id;

    @JsonProperty("author_id")

    private UUID authorId;
    private String title;
    private String content;
    private Status status;

    public static MyDto of(Manuscript m) {
        MyDto dto = new MyDto();
        dto.id = m.getId();
        dto.authorId = m.getAuthorId();
        dto.title = m.getTitle();
        dto.content = m.getContent();
        dto.status = m.getStatus();
        return dto;
    }
}
