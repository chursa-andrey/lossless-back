package fm.lossless.users.web;

import fm.lossless.auth.security.AuthPrincipal;
import fm.lossless.users.domain.User;
import fm.lossless.users.service.CurrentUserService;
import fm.lossless.users.web.dto.UserDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    public CurrentUserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal AuthPrincipal principal) {
        User user = currentUserService.getCurrentUser(principal);
        return ResponseEntity.ok(UserDto.from(user));
    }
}
