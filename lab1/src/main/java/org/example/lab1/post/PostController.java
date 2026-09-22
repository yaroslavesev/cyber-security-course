package org.example.lab1.post;

import java.security.Principal;
import java.util.List;

import org.example.lab1.user.AppUser;
import org.example.lab1.user.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api")
public class PostController {
    private final PostRepository postRepository;
    private final AppUserRepository userRepository;

    public PostController(PostRepository postRepository, AppUserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/data")
    public List<PostResponse> getData() {
        return postRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse createPost(@Valid @RequestBody CreatePostRequest request, Principal principal) {
        AppUser author = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        Post post = postRepository.save(new Post(request.title(), request.body(), author));
        return toResponse(post);
    }

    private PostResponse toResponse(Post post) {
        return new PostResponse(
                post.getId(),
                escape(post.getTitle()),
                escape(post.getBody()),
                escape(post.getAuthor().getUsername())
        );
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value);
    }

    public record CreatePostRequest(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 1000) String body
    ) {
    }

    public record PostResponse(Long id, String title, String body, String author) {
    }
}
