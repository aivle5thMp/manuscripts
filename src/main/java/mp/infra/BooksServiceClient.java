package mp.infra;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.UUID;

@FeignClient(name = "books", url = "${books.service.url:http://localhost:8084}")
public interface BooksServiceClient {
    
    @PostMapping("/books/create")
    BookCreateResponse createBook(@RequestBody BookCreateRequest request);
    
    @Data
    class BookCreateRequest {
        @JsonProperty("author_id")
        private UUID authorId;
        
        @JsonProperty("author_name")
        private String authorName;
        
        private String title;
        private Integer point;
        private String category;
        private String summary;
        private String content;
        
        @JsonProperty("image_url")
        private String imageUrl;
        
        @JsonProperty("audio_url")
        private String audioUrl;
    }
    
    @Data
    class BookCreateResponse {
        private boolean success;
        private String message;
        private UUID bookId;
    }
} 