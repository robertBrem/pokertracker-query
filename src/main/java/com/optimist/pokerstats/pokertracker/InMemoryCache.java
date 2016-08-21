package com.optimist.pokerstats.pokertracker;

import com.airhacks.porcupine.execution.boundary.Dedicated;
import com.optimist.pokerstats.pokertracker.account.entity.AccountPosition;
import com.optimist.pokerstats.pokertracker.account.event.AccountPositionCreated;
import com.optimist.pokerstats.pokertracker.account.event.AccountPositionEvent;
import com.optimist.pokerstats.pokertracker.player.entity.Player;
import com.optimist.pokerstats.pokertracker.player.event.PlayerCreated;
import com.optimist.pokerstats.pokertracker.player.event.PlayerDeleted;
import com.optimist.pokerstats.pokertracker.player.event.PlayerEvent;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class InMemoryCache {

    @Getter
    private Map<Long, Player> players = new HashMap<>();

    @Getter
    private Map<Long, AccountPosition> accountPositions = new HashMap<>();

    @Inject
    KafkaConsumer<String, String> consumer;

    @Inject
    KafkaProducer<String, String> producer;

    @Dedicated
    @Inject
    ExecutorService kafka;

    @Inject
    JsonConverter converter;

    @PostConstruct
    public void onInit() {
        String topicName = getTopicName();
        JsonObject event = Json.createObjectBuilder()
                .add("topicName", topicName)
                .build();
        producer.send(new ProducerRecord<>(
                KafkaProvider.TOPIC,
                event.toString()));
        consumer.subscribe(Arrays.asList(topicName));

        CompletableFuture
                .runAsync(this::handleKafkaEvent, kafka);
    }

    public String getTopicName() {
        InetAddress localHost = null;
        try {
            localHost = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return "replayAllFromStore" + localHost.getHostName();
    }

    public void handleKafkaEvent() {
        while (true) {
            ConsumerRecords<String, String> records = consumer.poll(1000);
            for (ConsumerRecord<String, String> record : records) {
                switch (record.topic()) {
                    case KafkaProvider.TOPIC:
                        System.out.println("record.value() = " + record.value());
                        List<CoreEvent> events = converter.convertToEvents(record.value());
                        for (CoreEvent event : events) {
                            handle(event);
                        }
                        break;
                    default:
                        System.out.println("record.value() " + record.topic() + " = " + record.value());
                        List<CoreEvent> events2 = converter.convertToEvents(record.value());
                        for (CoreEvent event : events2) {
                            handle(event);
                        }
                        break;
                }
            }
        }
    }

    public void handle(CoreEvent event) {
        if (event instanceof PlayerCreated) {
            ArrayList<CoreEvent> events = new ArrayList<>();
            events.add(event);
            Player player = new Player(events);
            players.put(event.getId(), player);
            System.out.println("players.size() = " + players.size());
        } else if (event instanceof PlayerDeleted) {
            Player player = players.get(event.getId());
            if (player == null) {
                System.out.println("rejected!");
                return;
            }
            players.remove(player.getId());
        } else if (event instanceof PlayerEvent) {
            Player player = players.get(event.getId());
            if (player == null) {
                System.out.println("rejected!");
                return;
            }
            player.mutate(event);
        } else if (event instanceof AccountPositionCreated) {
            ArrayList<CoreEvent> events = new ArrayList<>();
            events.add(event);
            AccountPosition accountPosition = new AccountPosition(events);
            accountPositions.put(event.getId(), accountPosition);
        } else if (event instanceof AccountPositionEvent) {
            AccountPosition accountPosition = accountPositions.get(event.getId());
            if (accountPosition == null) {
                System.out.println("rejected!");
                return;
            }
            accountPosition.mutate(event);
        } else {
            System.out.println("NOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
            throw new NotImplementedException();
        }
    }

}
