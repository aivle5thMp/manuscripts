package mp.infra;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import java.util.UUID;

@FeignClient(name = "authors", url = "${authors.service.url:http://localhost:8081}")
public interface AuthorServiceClient {
    
    @GetMapping("/authors/status")
    ApiResponse<AuthorDto> getMyAuthorStatus(@RequestHeader("Authorization") String authorization);
    
    class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        
        public ApiResponse() {}
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public T getData() { return data; }
        public void setData(T data) { this.data = data; }
    }
    
    class AuthorDto {
        private UUID id;
        private UUID userId;
        private String status;
        private String name;
        
        public AuthorDto() {}
        
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public UUID getUserId() { return userId; }
        public void setUserId(UUID userId) { this.userId = userId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
} 