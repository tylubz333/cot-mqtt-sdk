package de.tarent.telekom.cot.mqtt.util;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MQTTTestClient extends AbstractVerticle {

    private static final String MQTT_BT_SUBSCRIPTION = "ss/testDevice";
    private static final String MQTT_BT_PUBLISH = "sr/testDevice";

    private static final String MQTT_SUBSCRIPTION = "ms/testDevice";
    private static final String MQTT_PUBLISH = "mr/testDevice";
    private static final String TESTPW = "testPW";
    private static final String BROKER_HOST = "localhost";
    private static final int BROKER_PORT = 11883;

    private static final String MESSAGE = "15,sim770\\n410,OPID1,SUCCESSFUL,result of the successful command,ln -s";

    Logger logger = LoggerFactory.getLogger(MQTTTestClient.class);

    private boolean periodicPublishing = false;

    public static void main(String[] args) {
        Vertx.vertx().deployVerticle(new MQTTTestClient());
    }

    public MQTTTestClient() {
        this(true);
    }

    public MQTTTestClient(boolean startPeriodicPublishing) {
        periodicPublishing = startPeriodicPublishing;
    }

    @Override
    public void start() throws Exception {
        MqttClientOptions option = new MqttClientOptions();
        option.setUsername("testuser");
        option.setPassword("initPW");
        option.setClientId("endpointClient");
        MqttClient mqttClient = MqttClient.create(vertx, option);
        mqttClient.publishHandler(h -> {
            if (h.topicName().equals(MQTT_BT_SUBSCRIPTION)) {
                logger.info("Message for " + MQTT_BT_SUBSCRIPTION + " received.");
                Buffer plkey = h.payload();
                Secret secret = new Secret(plkey.getBytes());
                EncryptionHelper encHelper = new EncryptionHelper();
                byte[] toSend = encHelper.encrypt(secret, TESTPW.getBytes());
                mqttClient.publish(MQTT_BT_PUBLISH,
                    Buffer.buffer(toSend),
                    MqttQoS.AT_LEAST_ONCE,
                    false,
                    false,
                    finishHandler -> {
                        if (finishHandler.succeeded()) {
                            logger.info(finishHandler.result());
                        } else {
                            logger.error("Error during publishing password", finishHandler.cause());
                        }
                    });
            } else if (h.topicName().equals(MQTT_SUBSCRIPTION)) {
                final Buffer payload = h.payload();
                logger.info("Message for " + MQTT_SUBSCRIPTION + " received.");
                logger.info("Message-Body:" + payload.toString());
                if (payload.toString().contains("600,")) {
                    mqttClient.publish(MQTT_PUBLISH,
                        Buffer.buffer("15,mascot-testdevices1\n603,aaaa,bbbb"),
                        MqttQoS.AT_LEAST_ONCE,
                        false,
                        false,
                        finishHandler -> {
                            if (finishHandler.succeeded()) {
                                logger.info(finishHandler.result());
                            } else {
                                logger.error("Error during publishing message", finishHandler.cause());
                            }
                    });
                }
            }
        });
        mqttClient.connect(BROKER_PORT, BROKER_HOST, ch -> {
            if (ch.succeeded()) {
                logger.info("Connected to a server");
                mqttClient.subscribe(MQTT_BT_SUBSCRIPTION, MqttQoS.AT_LEAST_ONCE.value());
                mqttClient.subscribe(MQTT_SUBSCRIPTION, MqttQoS.AT_LEAST_ONCE.value());
                if (periodicPublishing) {
                    vertx.setPeriodic(1000, t -> {
                        mqttClient.publish(MQTT_PUBLISH,
                            Buffer.buffer(MESSAGE),
                            MqttQoS.AT_LEAST_ONCE,
                            false,
                            false,
                            finishHandler -> {
                                if (finishHandler.succeeded()) {
                                    logger.info(finishHandler.result());
                                } else {
                                    logger.error("Error during publishing message", finishHandler.cause());
                                }
                            });
                    });
                }
            } else {
                logger.error("Failed to connect to a server", ch.cause());
            }
        });

    }


}
