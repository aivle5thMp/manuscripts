package mp.infra;

import lombok.Data;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import mp.domain.Manuscript;
import mp.domain.ManuscriptRepository;
import mp.infra.service.PublishingService;
import mp.util.UserHeaderUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;

@RestController
@RequestMapping("/manuscripts")
@Transactional
public class ManuscriptController {

    @Autowired
    private ManuscriptRepository manuscriptRepository;
    
    @Autowired
    private AuthorServiceClient authorServiceClient;
    
    @Autowired
    private PublishingService publishingService;

    @Data
    static class CreateReq {
        private String title;
        private String content;
    }

    @Data
    static class IdRes {
        private boolean success;
        private UUID id;
        private String message;
    }

    @Data
    static class DetailRes {
        private boolean success;
        private String title;
        private String content;
        private String status;
        private String message;
    }

    @Data
    static class EditReq {
        private UUID id;
        private String title;
        private String content;
    }

    @Data
    static class DeleteReq {
        private UUID id;
    }

    @Data
    static class PublishReq {
        private UUID id;
    }

    // 현재 사용자의 작가 ID 조회
    private UUID getAuthorId(String authToken) {
        try {
            System.out.println("🔍 Calling authors service with token: " + authToken.substring(0, 20) + "...");
            
            AuthorServiceClient.ApiResponse<AuthorServiceClient.AuthorDto> response = 
                authorServiceClient.getMyAuthorStatus(authToken);
            
            System.out.println("📡 Authors service response - success: " + response.isSuccess() + 
                             ", message: " + response.getMessage());
                
            if (response.isSuccess() && response.getData() != null) {
                AuthorServiceClient.AuthorDto author = response.getData();
                System.out.println("👤 Author info - ID: " + author.getId() + 
                                 ", Status: " + author.getStatus() + 
                                 ", Name: " + author.getName());
                
                if ("APPROVED".equals(author.getStatus())) {
                    return author.getId();
                } else {
                    throw new RuntimeException("Author not approved. Status: " + author.getStatus());
                }
            } else {
                throw new RuntimeException("Author information not found");
            }
        } catch (Exception e) {
            System.err.println("❌ Error in getAuthorId: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to verify author status: " + e.getMessage());
        }
    }
    
    // 현재 사용자의 작가 정보 조회
    private AuthorServiceClient.AuthorDto getAuthorInfo(String authToken) {
        try {
            System.out.println("🔍 Calling authors service with token: " + authToken.substring(0, 20) + "...");
            
            AuthorServiceClient.ApiResponse<AuthorServiceClient.AuthorDto> response = 
                authorServiceClient.getMyAuthorStatus(authToken);
            
            System.out.println("📡 Authors service response - success: " + response.isSuccess() + 
                             ", message: " + response.getMessage());
                
            if (response.isSuccess() && response.getData() != null) {
                AuthorServiceClient.AuthorDto author = response.getData();
                System.out.println("👤 Author info - ID: " + author.getId() + 
                                 ", Status: " + author.getStatus() + 
                                 ", Name: " + author.getName());
                
                if ("APPROVED".equals(author.getStatus())) {
                    return author;
                } else {
                    throw new RuntimeException("Author not approved. Status: " + author.getStatus());
                }
            } else {
                throw new RuntimeException("Author information not found");
            }
        } catch (Exception e) {
            System.err.println("❌ Error in getAuthorInfo: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to verify author status: " + e.getMessage());
        }
    }

    // 1) 내 원고 목록 조회 (작가만)
    @GetMapping("/my")
    public ResponseEntity<?> getMyManuscripts(HttpServletRequest request, 
                                            @RequestHeader("Authorization") String authToken) {
        System.out.println("📋 =========================== GET MY MANUSCRIPTS ===========================");
        System.out.println("📋 Request URI: " + request.getRequestURI());
        System.out.println("📋 Request Method: " + request.getMethod());
        System.out.println("📋 Request Time: " + java.time.LocalDateTime.now());
        
        try {
            // 1. 사용자 인증 확인
            System.out.println("🔐 [AUTH STEP 1] 인증 상태 확인 중...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.err.println("❌ [AUTH STEP 1] 인증 실패 - 토큰이 없거나 유효하지 않음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            System.out.println("✅ [AUTH STEP 1] 인증 상태 확인 완료");
            
            // 2. 작가 권한 확인
            System.out.println("🔐 [AUTH STEP 2] 작가 권한 확인 중...");
            if (!UserHeaderUtil.isAuthor(request)) {
                System.err.println("❌ [AUTH STEP 2] 작가 권한 없음 - 현재 역할: " + UserHeaderUtil.getUserRole(request));
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("작가 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 2] 작가 권한 확인 완료");
            
            // 3. 작가 정보 조회
            System.out.println("🔍 [AUTH STEP 3] 작가 정보 조회 중...");
            UUID authorId = getAuthorId(authToken);
            System.out.println("✅ [AUTH STEP 3] 작가 정보 조회 완료 - Author ID: " + authorId);
            
            // 4. 원고 목록 조회
            System.out.println("📄 [QUERY] 원고 목록 조회 중...");
            List<Manuscript> manuscripts = manuscriptRepository.findByUserId(authorId);
            System.out.println("✅ [QUERY] 원고 목록 조회 완료 - 총 " + manuscripts.size() + "개 원고");
            
            System.out.println("🎉 ==================== GET MY MANUSCRIPTS SUCCESS ====================");
            return ResponseEntity.ok(manuscripts);
            
        } catch (Exception e) {
            System.err.println("❌ ==================== GET MY MANUSCRIPTS FAILED ====================");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            
            IdRes response = new IdRes();
            response.setSuccess(false);
            response.setMessage("원고 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 2) 원고 작성 (작가만)
    @PostMapping("/create")
    public ResponseEntity<IdRes> createManuscript(@RequestBody CreateReq req, HttpServletRequest request,
                                                @RequestHeader("Authorization") String authToken) {
        System.out.println("📝 =========================== CREATE MANUSCRIPT ===========================");
        System.out.println("📝 Request URI: " + request.getRequestURI());
        System.out.println("📝 Request Method: " + request.getMethod());
        System.out.println("📝 Request Time: " + java.time.LocalDateTime.now());
        System.out.println("📝 Title: " + req.getTitle());
        System.out.println("📝 Content Length: " + (req.getContent() != null ? req.getContent().length() : 0) + " chars");
        
        try {
            // 1. 사용자 인증 확인
            System.out.println("🔐 [AUTH STEP 1] 인증 상태 확인 중...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.err.println("❌ [AUTH STEP 1] 인증 실패");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            System.out.println("✅ [AUTH STEP 1] 인증 상태 확인 완료");
            
            // 2. 작가 권한 확인
            System.out.println("🔐 [AUTH STEP 2] 작가 권한 확인 중...");
            if (!UserHeaderUtil.isAuthor(request)) {
                System.err.println("❌ [AUTH STEP 2] 작가 권한 없음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("작가 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 2] 작가 권한 확인 완료");
            
            // 3. 작가 정보 조회
            System.out.println("🔍 [AUTH STEP 3] 작가 정보 조회 중...");
            UUID authorId = getAuthorId(authToken);
            System.out.println("✅ [AUTH STEP 3] 작가 정보 조회 완료 - Author ID: " + authorId);
            
            // 4. 원고 생성
            System.out.println("📄 [CREATE] 원고 생성 중...");
            Manuscript manuscript = new Manuscript();
            manuscript.setUserId(authorId);
            manuscript.setTitle(req.getTitle());
            manuscript.setContent(req.getContent());
            manuscript.setStatus(Manuscript.Status.DRAFT);
            
            Manuscript saved = manuscriptRepository.save(manuscript);
            System.out.println("✅ [CREATE] 원고 생성 완료 - ID: " + saved.getId());
            
            IdRes response = new IdRes();
            response.setSuccess(true);
            response.setId(saved.getId());
            response.setMessage("Manuscript created successfully");
            
            System.out.println("🎉 ==================== CREATE MANUSCRIPT SUCCESS ====================");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ==================== CREATE MANUSCRIPT FAILED ====================");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            
            IdRes response = new IdRes();
            response.setSuccess(false);
            response.setMessage("원고 생성 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 3) 원고 상세 조회 (작가 본인 원고만)
    @GetMapping("/detail/{id}")
    public ResponseEntity<DetailRes> getDetail(@PathVariable UUID id, HttpServletRequest request,
                                             @RequestHeader("Authorization") String authToken) {
        System.out.println("📖 =========================== GET MANUSCRIPT DETAIL ===========================");
        System.out.println("📖 Request URI: " + request.getRequestURI());
        System.out.println("📖 Manuscript ID: " + id);
        System.out.println("📖 Request Time: " + java.time.LocalDateTime.now());
        
        try {
            // 1. 사용자 인증 확인
            System.out.println("🔐 [AUTH STEP 1] 인증 상태 확인 중...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.err.println("❌ [AUTH STEP 1] 인증 실패");
                DetailRes response = new DetailRes();
                response.setSuccess(false);
                response.setMessage("인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            System.out.println("✅ [AUTH STEP 1] 인증 상태 확인 완료");
            
            // 2. 작가 권한 확인
            System.out.println("🔐 [AUTH STEP 2] 작가 권한 확인 중...");
            if (!UserHeaderUtil.isAuthor(request)) {
                System.err.println("❌ [AUTH STEP 2] 작가 권한 없음");
                DetailRes response = new DetailRes();
                response.setSuccess(false);
                response.setMessage("작가 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 2] 작가 권한 확인 완료");
            
            // 3. 작가 정보 조회
            System.out.println("🔍 [AUTH STEP 3] 작가 정보 조회 중...");
            UUID authorId = getAuthorId(authToken);
            System.out.println("✅ [AUTH STEP 3] 작가 정보 조회 완료 - Author ID: " + authorId);
            
            // 4. 원고 존재 확인
            System.out.println("📄 [QUERY] 원고 존재 확인 중...");
            Optional<Manuscript> manuscript = manuscriptRepository.findById(id);
            if (manuscript.isEmpty()) {
                System.err.println("❌ [QUERY] 원고를 찾을 수 없음 - ID: " + id);
                DetailRes response = new DetailRes();
                response.setSuccess(false);
                response.setMessage("Manuscript not found");
                return ResponseEntity.notFound().build();
            }
            System.out.println("✅ [QUERY] 원고 존재 확인 완료");
            
            Manuscript m = manuscript.get();
            System.out.println("📋 원고 정보 - Title: '" + m.getTitle() + "', Status: " + m.getStatus() + ", Owner: " + m.getUserId());
            
            // 5. 권한 확인 (본인 원고인지)
            System.out.println("🔐 [AUTH STEP 4] 원고 소유권 확인 중...");
            if (!m.getUserId().equals(authorId)) {
                System.err.println("❌ [AUTH STEP 4] 원고 소유권 없음");
                DetailRes response = new DetailRes();
                response.setSuccess(false);
                response.setMessage("Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 4] 원고 소유권 확인 완료");
            
            DetailRes response = new DetailRes();
            response.setSuccess(true);
            response.setTitle(m.getTitle());
            response.setContent(m.getContent());
            response.setStatus(m.getStatus().toString());
            response.setMessage("Success");
            
            System.out.println("🎉 ==================== GET MANUSCRIPT DETAIL SUCCESS ====================");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ==================== GET MANUSCRIPT DETAIL FAILED ====================");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            
            DetailRes response = new DetailRes();
            response.setSuccess(false);
            response.setMessage("원고 상세 조회 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 4) 원고 수정 (작가 본인 원고만)
    @PutMapping("/edit")
    public ResponseEntity<IdRes> editManuscript(@RequestBody EditReq req, HttpServletRequest request,
                                              @RequestHeader("Authorization") String authToken) {
        System.out.println("✏️ =========================== EDIT MANUSCRIPT ===========================");
        System.out.println("✏️ Request URI: " + request.getRequestURI());
        System.out.println("✏️ Manuscript ID: " + req.getId());
        System.out.println("✏️ New Title: " + req.getTitle());
        System.out.println("✏️ New Content Length: " + (req.getContent() != null ? req.getContent().length() : 0) + " chars");
        System.out.println("✏️ Request Time: " + java.time.LocalDateTime.now());
        
        try {
            // 1. 사용자 인증 확인
            System.out.println("🔐 [AUTH STEP 1] 인증 상태 확인 중...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.err.println("❌ [AUTH STEP 1] 인증 실패");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            System.out.println("✅ [AUTH STEP 1] 인증 상태 확인 완료");
            
            // 2. 작가 권한 확인
            System.out.println("🔐 [AUTH STEP 2] 작가 권한 확인 중...");
            if (!UserHeaderUtil.isAuthor(request)) {
                System.err.println("❌ [AUTH STEP 2] 작가 권한 없음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("작가 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 2] 작가 권한 확인 완료");
            
            // 3. 작가 정보 조회
            System.out.println("🔍 [AUTH STEP 3] 작가 정보 조회 중...");
            UUID authorId = getAuthorId(authToken);
            System.out.println("✅ [AUTH STEP 3] 작가 정보 조회 완료 - Author ID: " + authorId);
            
            // 4. 원고 존재 확인
            System.out.println("📄 [QUERY] 원고 존재 확인 중...");
            Optional<Manuscript> manuscript = manuscriptRepository.findById(req.getId());
            if (manuscript.isEmpty()) {
                System.err.println("❌ [QUERY] 원고를 찾을 수 없음 - ID: " + req.getId());
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("Manuscript not found");
                return ResponseEntity.notFound().build();
            }
            System.out.println("✅ [QUERY] 원고 존재 확인 완료");
            
            Manuscript m = manuscript.get();
            System.out.println("📋 기존 원고 정보 - Title: '" + m.getTitle() + "', Status: " + m.getStatus() + ", Owner: " + m.getUserId());
            
            // 5. 권한 확인 (본인 원고인지)
            System.out.println("🔐 [AUTH STEP 4] 원고 소유권 확인 중...");
            if (!m.getUserId().equals(authorId)) {
                System.err.println("❌ [AUTH STEP 4] 원고 소유권 없음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 4] 원고 소유권 확인 완료");
            
            // 6. 원고 수정
            System.out.println("📝 [UPDATE] 원고 수정 중...");
            m.setTitle(req.getTitle());
            m.setContent(req.getContent());
            manuscriptRepository.save(m);
            System.out.println("✅ [UPDATE] 원고 수정 완료");
            
            IdRes response = new IdRes();
            response.setSuccess(true);
            response.setId(m.getId());
            response.setMessage("Manuscript updated successfully");
            
            System.out.println("🎉 ==================== EDIT MANUSCRIPT SUCCESS ====================");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ==================== EDIT MANUSCRIPT FAILED ====================");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            
            IdRes response = new IdRes();
            response.setSuccess(false);
            response.setMessage("원고 수정 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 5) 원고 삭제 (작가 본인 원고만)
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<IdRes> deleteManuscript(@PathVariable UUID id, HttpServletRequest request,
                                                @RequestHeader("Authorization") String authToken) {
        System.out.println("🗑️ =========================== DELETE MANUSCRIPT ===========================");
        System.out.println("🗑️ Request URI: " + request.getRequestURI());
        System.out.println("🗑️ Manuscript ID: " + id);
        System.out.println("🗑️ Request Time: " + java.time.LocalDateTime.now());
        
        try {
            // 1. 사용자 인증 확인
            System.out.println("🔐 [AUTH STEP 1] 인증 상태 확인 중...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.err.println("❌ [AUTH STEP 1] 인증 실패");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            System.out.println("✅ [AUTH STEP 1] 인증 상태 확인 완료");
            
            // 2. 작가 권한 확인
            System.out.println("🔐 [AUTH STEP 2] 작가 권한 확인 중...");
            if (!UserHeaderUtil.isAuthor(request)) {
                System.err.println("❌ [AUTH STEP 2] 작가 권한 없음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("작가 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 2] 작가 권한 확인 완료");
            
            // 3. 작가 정보 조회
            System.out.println("🔍 [AUTH STEP 3] 작가 정보 조회 중...");
            UUID authorId = getAuthorId(authToken);
            System.out.println("✅ [AUTH STEP 3] 작가 정보 조회 완료 - Author ID: " + authorId);
            
            // 4. 원고 존재 확인
            System.out.println("📄 [QUERY] 원고 존재 확인 중...");
            Optional<Manuscript> manuscript = manuscriptRepository.findById(id);
            if (manuscript.isEmpty()) {
                System.err.println("❌ [QUERY] 원고를 찾을 수 없음 - ID: " + id);
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("Manuscript not found");
                return ResponseEntity.notFound().build();
            }
            System.out.println("✅ [QUERY] 원고 존재 확인 완료");
            
            Manuscript m = manuscript.get();
            System.out.println("📋 원고 정보 - Title: '" + m.getTitle() + "', Status: " + m.getStatus() + ", Owner: " + m.getUserId());
            
            // 5. 권한 확인 (본인 원고인지)
            System.out.println("🔐 [AUTH STEP 4] 원고 소유권 확인 중...");
            if (!m.getUserId().equals(authorId)) {
                System.err.println("❌ [AUTH STEP 4] 원고 소유권 없음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 4] 원고 소유권 확인 완료");
            
            // 6. 원고 삭제
            System.out.println("🗑️ [DELETE] 원고 삭제 중...");
            manuscriptRepository.deleteById(id);
            System.out.println("✅ [DELETE] 원고 삭제 완료");
            
            IdRes response = new IdRes();
            response.setSuccess(true);
            response.setId(id);
            response.setMessage("Manuscript deleted successfully");
            
            System.out.println("🎉 ==================== DELETE MANUSCRIPT SUCCESS ====================");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ==================== DELETE MANUSCRIPT FAILED ====================");
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
            
            IdRes response = new IdRes();
            response.setSuccess(false);
            response.setMessage("원고 삭제 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // 6) 출간 신청 (작가 본인 원고만) - 즉시 응답
    @PostMapping("/publish/{id}")
    public ResponseEntity<IdRes> publishRequest(@PathVariable UUID id, HttpServletRequest request,
                                              @RequestHeader("Authorization") String authToken) {
        System.out.println("📝 =========================== PUBLISH REQUEST START ===========================");
        System.out.println("📝 Request URI: " + request.getRequestURI());
        System.out.println("📝 Manuscript ID: " + id);
        System.out.println("📝 Request Time: " + java.time.LocalDateTime.now());
        
        try {
            // 1. 사용자 인증 확인
            System.out.println("🔐 [AUTH STEP 1] 인증 상태 확인 중...");
            if (!UserHeaderUtil.isAuthenticated(request)) {
                System.err.println("❌ [AUTH STEP 1] 인증 실패");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("인증이 필요합니다.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            System.out.println("✅ [AUTH STEP 1] 인증 상태 확인 완료");
            
            // 2. 작가 권한 확인
            System.out.println("🔐 [AUTH STEP 2] 작가 권한 확인 중...");
            if (!UserHeaderUtil.isAuthor(request)) {
                System.err.println("❌ [AUTH STEP 2] 작가 권한 없음");
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("작가 권한이 필요합니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [AUTH STEP 2] 작가 권한 확인 완료");
            
            // 3. 작가 정보 확인
            System.out.println("🔍 [STEP 3] 작가 정보 확인 중...");
            AuthorServiceClient.AuthorDto authorInfo = getAuthorInfo(authToken);
            UUID authorId = authorInfo.getId();
            String authorName = authorInfo.getName();
            System.out.println("✅ [STEP 3] 작가 정보 확인 완료 - Author ID: " + authorId + ", Name: " + authorName);
            
            // 4. 원고 존재 여부 확인
            System.out.println("📄 [STEP 4] 원고 존재 여부 확인 중...");
            Optional<Manuscript> manuscript = manuscriptRepository.findById(id);
            if (manuscript.isEmpty()) {
                System.err.println("❌ [STEP 4] 원고를 찾을 수 없음 - ID: " + id);
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("Manuscript not found");
                return ResponseEntity.notFound().build();
            }
            System.out.println("✅ [STEP 4] 원고 확인 완료");
            
            Manuscript m = manuscript.get();
            System.out.println("📋 원고 정보 - Title: '" + m.getTitle() + "', Status: " + m.getStatus() + ", Content Length: " + (m.getContent() != null ? m.getContent().length() : 0) + " chars");
            
            // 5. 권한 확인
            System.out.println("🔐 [STEP 5] 권한 확인 중...");
            if (!m.getUserId().equals(authorId)) {
                System.err.println("❌ [STEP 5] 권한 없음 - 원고 소유자 ID: " + m.getUserId() + ", 요청자 ID: " + authorId);
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("Access denied");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            System.out.println("✅ [STEP 5] 권한 확인 완료");
            
            // 6. 상태 확인 (DRAFT 또는 SUBMITTED만 출간 가능)
            System.out.println("📋 [STEP 6] 원고 상태 확인 중...");
            if (m.getStatus() != Manuscript.Status.DRAFT && m.getStatus() != Manuscript.Status.SUBMITTED) {
                System.err.println("❌ [STEP 6] 출간 불가능한 상태 - Current Status: " + m.getStatus());
                IdRes response = new IdRes();
                response.setSuccess(false);
                response.setMessage("이미 출간 처리 중이거나 완료된 원고입니다. 현재 상태: " + m.getStatus());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            System.out.println("✅ [STEP 6] 원고 상태 확인 완료 - 출간 처리 가능");
            
            // 7. 상태를 출간검토중으로 변경
            System.out.println("📝 [STEP 7] 원고 상태를 UNDER_REVIEW로 변경 중...");
            Manuscript.Status oldStatus = m.getStatus();
            m.setStatus(Manuscript.Status.UNDER_REVIEW);
            manuscriptRepository.save(m);
            System.out.println("✅ [STEP 7] 원고 상태 변경 완료: " + oldStatus + " → " + Manuscript.Status.UNDER_REVIEW);
            
            // 8. 비동기 처리 시작
            System.out.println("🚀 [STEP 8] 비동기 출간 처리 시작...");
            publishingService.processPublishingAsync(id, authorId, authorName, authToken);
            System.out.println("✅ [STEP 8] 비동기 처리 시작됨");
            
            // 9. 즉시 응답
            IdRes response = new IdRes();
            response.setSuccess(true);
            response.setId(m.getId());
            response.setMessage("출간 검토가 시작되었습니다. 처리 완료까지 시간이 소요될 수 있습니다.");
            
            System.out.println("🎉 ==================== PUBLISH REQUEST ACCEPTED ====================");
            System.out.println("🎉 상태: UNDER_REVIEW");
            System.out.println("🎉 비동기 처리 시작됨");
            System.out.println("🎉 ================================================================");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.err.println("❌ ====================== PUBLISH REQUEST FAILED ======================");
            System.err.println("❌ Error in publishRequest: " + e.getMessage());
            System.err.println("❌ Exception Type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            System.err.println("❌ =================================================================");
            
            IdRes response = new IdRes();
            response.setSuccess(false);
            response.setMessage("출간 요청 처리 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}

