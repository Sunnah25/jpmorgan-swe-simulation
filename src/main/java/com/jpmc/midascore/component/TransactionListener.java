package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.IncentiveResponse;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Component
public class TransactionListener {

    @Autowired
    private UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    public void listen(Transaction transaction) {
        // Step 1: look up sender and recipient
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        // Step 2: if either doesn't exist, ignore
        if (sender == null || recipient == null) {
            return;
        }

        // Step 3: check sender has enough balance
        if (sender.getBalance() < transaction.getAmount()) {
            return;
        }

        // Step 4: call the Incentive API
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Transaction> request = new HttpEntity<>(transaction, headers);
        IncentiveResponse incentive = restTemplate.postForObject(
            "http://localhost:8080/incentive",
            request,
            IncentiveResponse.class
        );

        float bonus = (incentive != null) ? incentive.getAmount() : 0;

        // Step 5: update balances
        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + bonus);

        // Step 6: save both users
        userRepository.save(sender);
        userRepository.save(recipient);
    }
}