package com.optimist.pokerstats.pokertracker;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import javax.annotation.PostConstruct;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Properties;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class KafkaProvider {
    public static final String TOPIC = "pokertracker";
    public static final String KAFKA_ADDRESS = "KAFKA_ADDRESS";

    private KafkaConsumer<String, String> consumer;

    @Inject
    EnvironmentVariableGetter envGetter;

    @PostConstruct
    public void init() {
        this.consumer = createConsumer();
    }

    @Produces
    public KafkaConsumer<String, String> getConsumer() {
        return consumer;
    }

    public String getKafkaAddress() {
        String address = "localhost:9092";
        String kafkaEnv = envGetter.getEnv(KAFKA_ADDRESS);
        if (kafkaEnv != null && !kafkaEnv.isEmpty()) {
            address = kafkaEnv;
        }
        return address;
    }

    public KafkaConsumer<String, String> createConsumer() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", getKafkaAddress());
        properties.put("group.id", "pokertrackerquery");
        properties.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        consumer.subscribe(Arrays.asList(TOPIC));
        return consumer;
    }

}