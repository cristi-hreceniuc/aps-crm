package rotld.apscrm.services;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailSenderService {
    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String EMAIL;

    public void sendEmailWithPdfAttachment(String sendTo, String subject, String setMessage, String path) throws MessagingException {

        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true);
        String emailContent = setMessage;

        helper.setFrom(EMAIL);
        helper.setTo(sendTo);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        try {
            ByteArrayDataSource attachment = new ByteArrayDataSource(new URL(path).openStream(), "application/octet-stream");
            helper.addAttachment(subject + ".pdf", attachment);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        embedCidImagesIfPresent(emailContent, helper);

        javaMailSender.send(msg);
        log.info("Successfully sent mail with attachment to <{}>.", sendTo);
    }

    public void sendEmail(String sendTo, String subject, String setMessage) throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = javaMailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");

        String emailContent = setMessage;

        helper.setFrom("ostafie.ionut@gmail.com", "Acțiune pentru Sănătate");
        helper.setTo(sendTo);
        helper.setSubject(subject);
        helper.setText(emailContent, true);

        embedCidImagesIfPresent(emailContent, helper);

        javaMailSender.send(msg);

        log.info("Successfully sent mail to <{}>.", sendTo);
    }

    private void embedCidImagesIfPresent(String emailContent, MimeMessageHelper helper) throws MessagingException {
        Document document = Jsoup.parse(emailContent);
        for (Element img : document.select("img[src^=cid:]")) {
            String cid = img.attr("src").substring(4); // Extract the part after `cid:`

            // Construct the assumed image file path
            String imagePath = "images/" + cid; // Assuming images are in `src/main/resources/images/`

            // Load the resource and add it to the email
            Resource imageResource = new ClassPathResource(imagePath);
            if (imageResource.exists()) {
                helper.addInline(cid, imageResource);
            } else {
                throw new IllegalArgumentException("Image not found for cid: " + cid);
            }
        }
    }
}
