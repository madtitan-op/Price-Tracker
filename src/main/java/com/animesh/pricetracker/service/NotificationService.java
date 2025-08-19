package com.animesh.pricetracker.service;

import com.animesh.pricetracker.model.TrackedProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    public void sendPriceAlert(TrackedProduct product) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(product.getUserEmail());
        message.setSubject("Price Alert! Product: " + product.getUrl());
        message.setText("The price for your tracked product has dropped! Check it out here: " + product.getUrl());

        mailSender.send(message);

        System.out.println("Price alert email sent to: " + product.getUserEmail());
    }
}
