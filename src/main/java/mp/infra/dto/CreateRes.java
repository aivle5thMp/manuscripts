package mp.infra.dto;

import java.util.UUID;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@AllArgsConstructor
public class CreateRes {
    @JsonProperty("script_id")
    private UUID id;

}
