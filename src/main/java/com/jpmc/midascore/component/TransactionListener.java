package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.IncentiveResponse;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class TransactionListener {

    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${general.incentive-api-url}")
    private String incentiveApiUrl;

    public TransactionListener(UserRepository userRepository, RestTemplate restTemplate) {
        this.userRepository = userRepository;
        this.restTemplate = restTemplate;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    public void listen(Transaction transaction) {
        logger.info("Received transaction: senderId={}, recipientId={}, amount={}",
                transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());

        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null || recipient == null) {
            logger.warn("Transaction rejected - user not found: senderId={}, recipientId={}",
                    transaction.getSenderId(), transaction.getRecipientId());
            return;
        }

        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Transaction rejected - insufficient funds: sender={}, balance={}, amount={}",
                    sender.getName(), sender.getBalance(), transaction.getAmount());
            return;
        }

        float bonus = getIncentiveBonus(transaction);

        sender.setBalance(sender.getBalance() - transaction.getAmount());
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + bonus);

        userRepository.save(sender);
        userRepository.save(recipient);

        logger.info("Transaction processed: {} -> {}, amount={}, bonus={}",
                sender.getName(), recipient.getName(), transaction.getAmount(), bonus);
    }

    private float getIncentiveBonus(Transaction transaction) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Transaction> request = new HttpEntity<>(transaction, headers);
            IncentiveResponse incentive = restTemplate.postForObject(incentiveApiUrl, request, IncentiveResponse.class);
            return (incentive != null) ? incentive.getAmount() : 0;
        } catch (Exception e) {
            logger.error("Failed to get incentive bonus, defaulting to 0: {}", e.getMessage());
            return 0;
        }
    }
}