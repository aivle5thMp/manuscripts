package mp.infra.service;

import mp.domain.Manuscript;
import mp.domain.ManuscriptRepository;
import mp.infra.AIServiceClient;
import mp.infra.BooksServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class PublishingService {

    @Autowired
    private ManuscriptRepository manuscriptRepository;
    
    @Autowired
    private AIServiceClient aiServiceClient;
    
    @Autowired
    private BooksServiceClient booksServiceClient;

    @Async
    @Transactional
    public void processPublishingAsync(UUID manuscriptId, UUID authorId, String authorName, String authToken) {
        System.out.println("🔄 =========================== ASYNC PUBLISHING START ===========================");
        System.out.println("🔄 Manuscript ID: " + manuscriptId);
        System.out.println("🔄 Author ID: " + authorId);
        System.out.println("🔄 Author Name: " + authorName);
        System.out.println("🔄 Processing Time: " + java.time.LocalDateTime.now());
        
        try {
            // 1. 원고 다시 조회 (최신 상태 확인)
            System.out.println("📄 [ASYNC STEP 1] 원고 재조회 중...");
            Optional<Manuscript> manuscript = manuscriptRepository.findById(manuscriptId);
            if (manuscript.isEmpty()) {
                System.err.println("❌ [ASYNC STEP 1] 원고를 찾을 수 없음 - ID: " + manuscriptId);
                // 원고가 없으면 처리 중단
                return;
            }
            
            Manuscript m = manuscript.get();
            System.out.println("✅ [ASYNC STEP 1] 원고 재조회 완료 - Status: " + m.getStatus());
            
            // 상태가 UNDER_REVIEW가 아니면 처리 중단 (이미 다른 상태로 변경됨)
            if (m.getStatus() != Manuscript.Status.UNDER_REVIEW) {
                System.out.println("⚠️ [ASYNC STEP 1] 원고 상태가 UNDER_REVIEW가 아님. 처리 중단. Current Status: " + m.getStatus());
                return;
            }
            
            // 2. AI 서비스 호출하여 메타데이터 생성
            System.out.println("🤖 [ASYNC STEP 2] AI 서비스 호출 시작...");
            System.out.println("📤 AI 요청 데이터:");
            System.out.println("   - Title: " + m.getTitle());
            System.out.println("   - Author: " + authorName);
            System.out.println("   - Content Length: " + (m.getContent() != null ? m.getContent().length() : 0) + " chars");
            
            long aiStartTime = System.currentTimeMillis();
            AIServiceClient.AIRequest aiRequest = new AIServiceClient.AIRequest();
            aiRequest.setTitle(m.getTitle());
            aiRequest.setAuthor_name(authorName);
            aiRequest.setContent(m.getContent());
            
            AIServiceClient.AIResponse aiResponse = aiServiceClient.generateMetadata(aiRequest);
            long aiEndTime = System.currentTimeMillis();
            
            System.out.println("✅ [ASYNC STEP 2] AI 서비스 응답 완료 (소요시간: " + (aiEndTime - aiStartTime) + "ms)");
            System.out.println("📥 AI 응답 데이터:");
            System.out.println("   - Summary: " + (aiResponse.getSummary() != null ? aiResponse.getSummary().substring(0, Math.min(50, aiResponse.getSummary().length())) + "..." : "null"));
            System.out.println("   - Category: " + aiResponse.getCategory());
            System.out.println("   - Point: " + aiResponse.getPoint());
            System.out.println("   - Image URL: " + (aiResponse.getImage_url() != null ? "제공됨" : "없음"));
            System.out.println("   - Audio URL: " + (aiResponse.getAudio_url() != null ? "제공됨" : "없음"));
            
            // 3. Books 서비스 호출하여 책 등록
            System.out.println("📚 [ASYNC STEP 3] Books 서비스 호출 시작...");
            BooksServiceClient.BookCreateRequest bookRequest = new BooksServiceClient.BookCreateRequest();
            bookRequest.setAuthorId(authorId);
            bookRequest.setAuthorName(authorName);
            bookRequest.setTitle(m.getTitle());
            bookRequest.setPoint(aiResponse.getPoint());
            bookRequest.setCategory(aiResponse.getCategory());
            bookRequest.setSummary(aiResponse.getSummary());
            bookRequest.setContent(m.getContent());
            bookRequest.setImageUrl(aiResponse.getImage_url());
            bookRequest.setAudioUrl(aiResponse.getAudio_url());
            
            System.out.println("📤 Books 요청 데이터:");
            System.out.println("   - Author ID: " + authorId);
            System.out.println("   - Author Name: " + authorName);
            System.out.println("   - Title: " + m.getTitle());
            System.out.println("   - Point: " + aiResponse.getPoint());
            System.out.println("   - Category: " + aiResponse.getCategory());
            
            long booksStartTime = System.currentTimeMillis();
            BooksServiceClient.BookCreateResponse bookResponse = booksServiceClient.createBook(bookRequest);
            long booksEndTime = System.currentTimeMillis();
            
            System.out.println("📥 [ASYNC STEP 3] Books 서비스 응답 완료 (소요시간: " + (booksEndTime - booksStartTime) + "ms)");
            System.out.println("   - Success: " + bookResponse.isSuccess());
            System.out.println("   - Message: " + bookResponse.getMessage());
            System.out.println("   - Book ID: " + bookResponse.getBookId());
            
            // 4. 최종 상태 업데이트
            System.out.println("📝 [ASYNC STEP 4] 최종 상태 업데이트 중...");
            if (bookResponse.isSuccess()) {
                m.setStatus(Manuscript.Status.PUBLISHED);
                manuscriptRepository.save(m);
                System.out.println("✅ [ASYNC STEP 4] 원고 상태 업데이트 완료: UNDER_REVIEW → PUBLISHED");
                System.out.println("🎉 ======================== ASYNC PUBLISH SUCCESS ========================");
                System.out.println("🎉 Book ID: " + bookResponse.getBookId());
                System.out.println("🎉 Total Time: " + (booksEndTime - aiStartTime) + "ms");
                System.out.println("🎉 =================================================================");
            } else {
                m.setStatus(Manuscript.Status.REJECTED);
                manuscriptRepository.save(m);
                System.err.println("❌ [ASYNC STEP 4] Books 서비스에서 책 생성 실패, 상태를 REJECTED로 변경: " + bookResponse.getMessage());
                System.err.println("❌ ======================== ASYNC PUBLISH FAILED ========================");
            }
            
        } catch (Exception e) {
            System.err.println("❌ ====================== ASYNC PUBLISH ERROR ======================");
            System.err.println("❌ Error in processPublishingAsync: " + e.getMessage());
            System.err.println("❌ Exception Type: " + e.getClass().getSimpleName());
            e.printStackTrace();
            
            // 예외 발생 시 상태를 REJECTED로 변경
            try {
                Optional<Manuscript> manuscript = manuscriptRepository.findById(manuscriptId);
                if (manuscript.isPresent()) {
                    Manuscript m = manuscript.get();
                    m.setStatus(Manuscript.Status.REJECTED);
                    manuscriptRepository.save(m);
                    System.err.println("❌ 오류로 인해 원고 상태를 REJECTED로 변경함");
                }
            } catch (Exception saveException) {
                System.err.println("❌ 상태 변경 중 추가 오류 발생: " + saveException.getMessage());
            }
            System.err.println("❌ ========================================================");
        }
    }
} 