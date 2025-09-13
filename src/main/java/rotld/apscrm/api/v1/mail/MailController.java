package rotld.apscrm.api.v1.mail;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rotld.apscrm.api.v1.mail.dto.EmailRequestDto;
import rotld.apscrm.api.v1.user.dto.RegisterUserDto;
import rotld.apscrm.api.v1.user.repository.User;
import rotld.apscrm.services.EmailSenderService;

import java.io.UnsupportedEncodingException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mail")
@Slf4j
public class MailController {

    private final EmailSenderService emailSenderService;

    @PostMapping
    public void register(@RequestBody EmailRequestDto emailRequestDto) throws MessagingException, UnsupportedEncodingException {
        emailSenderService.sendEmail(
                emailRequestDto.sendTo(),
                emailRequestDto.subject(),
                emailRequestDto.message()
        );
    }
}
