package mp.infra;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import lombok.Data;

@FeignClient(name = "aiservice", url = "${ai.service.url:http://localhost:8083}")
public interface AIServiceClient {
    
    @PostMapping("/ai")
    AIResponse generateMetadata(@RequestBody AIRequest request);
    
    @Data
    class AIRequest {
        private String title;
        private String author_name;
        private String content;
    }
    
    @Data
    class AIResponse {
        private String summary;
        private String category;
        private Integer point;
        private String image_url;
        private String audio_url;
    }
} 