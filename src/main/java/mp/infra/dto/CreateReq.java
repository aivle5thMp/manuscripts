package mp.infra.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

@Data
@NoArgsConstructor
public class CreateReq {
    @JsonProperty("author_id")
    private UUID authorId;
    private String title;
    private String content;

}
