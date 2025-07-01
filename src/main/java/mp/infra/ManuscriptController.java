package mp.infra;

import java.util.*;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import mp.domain.*;
import mp.service.ManuscriptService;
import mp.infra.dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

//<<< Clean Arch / Inbound Adaptor

@RestController
@RequestMapping(value = "/manuscripts")
public class ManuscriptController {

    private final ManuscriptService service;

    @Autowired
    private ManuscriptController(ManuscriptService service) {
        this.service = service;
    }

    @GetMapping("/my")
    public List<MyDto> myList(@RequestParam("author_id") UUID authorId) {
        return service.getMyManuscripts(authorId)
                .stream()
                .map(MyDto::of)
                .collect(Collectors.toList());
    }

    @PostMapping("/create")
    public CreateRes create(@RequestBody CreateReq req) {
        return service.createManuscript(req);
    }

    @GetMapping("/detail")
    public MyDto detail(
            @RequestParam("id") UUID id,
            @RequestParam("author_id") UUID authorId
    )
    {
        return service.getDetail(id, authorId)
                .map(MyDto::of)
                .orElseThrow(() -> new RuntimeException("원고가 없습니다."));
    }

    @PutMapping("/edit")
    public void edit(@RequestBody EditReq r) {
        service.edit(r.getId(), r.getAuthorId(), r.getTitle(), r.getContent());
    }

    @DeleteMapping("/delete")
    public void delete(@RequestBody DeleteReq r) {
        service.delete(r.getId(), r.getAuthorId());
    }

    @PostMapping("/publish")
    public void publish(@RequestBody PublishReq r) {
        service.publish(r.getId(), r.getAuthorId());
    }
}
//>>> Clean Arch / Inbound Adaptor
