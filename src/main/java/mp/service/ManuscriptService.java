package mp.service;

import mp.domain.Manuscript;
import mp.domain.ManuscriptRepository;
import mp.domain.Status;
import mp.infra.dto.CreateReq;
import mp.infra.dto.CreateRes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ManuscriptService {

    private final ManuscriptRepository repository;

    @Autowired
    private ManuscriptService(ManuscriptRepository repository) {
        this.repository = repository;
    };

    public CreateRes createManuscript(CreateReq req) {
        Manuscript m = new Manuscript();
        m.setAuthorId(req.getAuthorId());
        m.setTitle(req.getTitle());
        m.setContent(req.getContent());
        m.setStatus(Status.DRAFT);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());

        Manuscript saved = repository.save(m);
        return new CreateRes(saved.getId());
    }

    public List<Manuscript> getMyManuscripts(UUID authorId) {
        return repository.findByAuthorId(authorId);
    }

    public Optional<Manuscript> getDetail(UUID id, UUID authorId) {
        return repository.findByIdAndAuthorId(id, authorId);
    }

    public void edit(UUID id, UUID authorId, String title, String content) {
        Manuscript m = repository
                .findByIdAndAuthorId(id, authorId)
                        .orElseThrow(() -> new RuntimeException("해당 원고를 찾을 수 없습니다."));
        m.setTitle(title);
        m.setContent(content);
        m.setUpdatedAt(LocalDateTime.now());
        repository.save(m);
    }

    public void delete(UUID id, UUID authorId) {
        Manuscript m = repository
                .findByIdAndAuthorId(id, authorId)
                .orElseThrow(() -> new RuntimeException("해당 원고를 찾을 수 없습니다."));
        repository.delete(m);
    }

    public void publish(UUID id, UUID authorId) {
        Manuscript m = repository
                .findByIdAndAuthorId(id, authorId)
                .orElseThrow(() -> new RuntimeException("해당 원고를 찾을 수 없습니다."));
        m.setStatus(Status.SUBMITTED);
        m.setUpdatedAt(LocalDateTime.now());
        repository.save(m);
    }
}


