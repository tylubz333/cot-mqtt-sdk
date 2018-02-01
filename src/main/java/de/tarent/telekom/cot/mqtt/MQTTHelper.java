package de.tarent.telekom.cot.mqtt;

import de.tarent.telekom.cot.mqtt.util.JsonHelper;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.Properties;
import java.util.function.Consumer;

public class MQTTHelper extends AbstractVerticle {

    static Logger logger = LoggerFactory.getLogger(MQTTHelper.class);
    static MQTTHelper helper;


    /**
        Starts the included verticles
     */
    private static void initAPI(JsonObject prop){
        Vertx v = Vertx.vertx();
        Configuration config = new Configuration();
        v.deployVerticle(config);
        BootstrapVerticle btvert = new BootstrapVerticle();
        v.deployVerticle(btvert);
        helper = new MQTTHelper();
        v.deployVerticle(helper);
        logger.info("Verticles started");
    }

    /**
     * Method returns an MQTTHelper instance and starts the vertx instance if not done before
     * @return MQTTHelpder instance
     */
    public static MQTTHelper getInstance(Properties properties){
        if (helper == null){
            JsonObject o = JsonHelper.from(properties);
            logger.info(o.encodePrettily());
            initAPI(o);
        }
        return helper;
    }

    /**
     * Method returns an MQTTHelper instance and starts the vertx instance if not done before
     * @return MQTTHelpder instance
     */
    public static MQTTHelper getInstance(JsonObject properties){
        if (helper == null){
            initAPI(properties);
        }
        return helper;
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        helper = null;
    }

    public void registerDevice(String deviceId, Consumer callback){
        EventBus eb = vertx.eventBus();
        JsonObject msg = new JsonObject().put("deviceId", deviceId);
        eb.send("register", msg, result ->{
            if (result.succeeded()){
                JsonObject regresult = (JsonObject)result.result().body();
                //ToDo:prepare ReturnMSG
                callback.accept(regresult.encodePrettily());
            }else{
                logger.error("Registration failed - ", result.cause());
            }
        });
    }

}
