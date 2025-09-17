package rotld.apscrm.api.v1.mail.service;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rotld.apscrm.api.v1.f230.repository.F230;
import rotld.apscrm.api.v1.f230.service.F230Service;
import rotld.apscrm.services.EmailSenderService;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class Formular230ReminderJob {

    private final F230Service f230Service;
    private final EmailSenderService emailSenderService;

    // rulează în fiecare an pe
    @Scheduled(cron = "0 0 10 10 1 *")   // 10 ianuarie, ora 10:00
    @Scheduled(cron = "0 0 10 1 5 *")   // 10 mai, ora 10:00
    public void sendReminders() {
        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();

        List<F230> forms = f230Service.getAll(); // creezi un listAll în service dacă nu ai
        for (F230 form : forms) {
            try {
                int formYear = Integer.parseInt(form.getYear());   // anul pentru care a fost completat
                int duration = Objects.equals(form.getDistrib2(), "1") ? 2 : 1; // 1 sau 2 ani

                if (currentYear == formYear + duration & Objects.equals(form.getEmail(), "cristianhreceniuc1@gmail.com")) {
                    String subject = "Reînnoire Formular 230 - Asociația Acțiune pentru Sănătate";
                    String emailContent = buildEmail(form, duration, "renewal_formular230.html");

                    emailSenderService.sendEmail(form.getEmail(), subject, emailContent);
                    log.info("Reminder trimis către {}", form.getEmail());
                }
            } catch (Exception e) {
                log.error("Eroare la procesarea formularului {}", form.getId(), e);
            }
        }
    }

    @Scheduled(cron = "0 0 10 * * *")
    public void sendBirthdayEmails() {
        LocalDate today = LocalDate.now();
        List<F230> forms = f230Service.getAll(); // creezi un listAll în service dacă nu ai

        for (F230 form : forms) {
            try {
                String cnp = form.getCnp(); // asigură-te că în DTO ai câmpul CNP
                if (cnp == null || cnp.length() < 7) continue;

                // extrage luna și ziua din CNP
                int month = Integer.parseInt(cnp.substring(3, 5));
                int day = Integer.parseInt(cnp.substring(5, 7));

                if (today.getMonthValue() == month && today.getDayOfMonth() == day) {
                    String subject = "🎂 La Mulți Ani din partea Asociației Acțiune pentru Sănătate!";
                    String emailContent = buildEmail(form, 1, "birthday.html");

                    emailSenderService.sendEmail(form.getEmail(), subject, emailContent);
                    log.info("Email de La Mulți Ani trimis către {}.)", form.getEmail());
                }
            } catch (MessagingException | UnsupportedEncodingException e) {
                log.error("Eroare la trimiterea emailului de ziua cuiva", e);
            } catch (Exception ex) {
                log.warn("CNP invalid pentru {}", form.getId());
            }
        }
    }

    private String buildEmail(F230 form, int duration, String templateName) {
        String template = loadTemplate(templateName); // încarci din resources/email_templates/formular230.html

        return template
                .replace("{{user_name}}", form.getFirstName() + " " + form.getLastName())
                .replace("{{years_since_completion}}", String.valueOf(duration))
                .replace("{{user_email}}", form.getEmail())
                .replace("{{website_url}}", "https://actiunepentrusanatate.ro")
                .replace("{{facebook_url}}", "https://facebook.com/actiunepentrusanatate")
                .replace("{{instagram_url}}", "https://instagram.com/actiunepentrusanatate")
                .replace("{{linkedin_url}}", "https://linkedin.com/company/actiunepentrusanatate")
                .replace("{{unsubscribe_url}}", "https://actiunepentrusanatate.ro/unsubscribe");
    }

    private String loadTemplate(String name) {
        try {
            return new String(
                    getClass().getResourceAsStream("/template/%s".formatted(name)).readAllBytes()
            );
        } catch (Exception e) {
            throw new RuntimeException("Nu pot încărca template email", e);
        }
    }
}
