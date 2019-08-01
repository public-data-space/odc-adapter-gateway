package de.fraunhofer.fokus.ids.services.webclient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.client.WebClient;

public class WebClientServiceImpl implements WebClientService {

    private Logger LOGGER = LoggerFactory.getLogger(WebClientServiceImpl.class.getName());

    private WebClient webClient;

    public WebClientServiceImpl(WebClient webClient, Handler<AsyncResult<WebClientService>> readyHandler) {
        this.webClient = webClient;
        readyHandler.handle(Future.succeededFuture(this));
    }

    @Override
    public WebClientService post(int port, String host, String path, JsonObject payload, Handler<AsyncResult<JsonObject>> resultHandler) {
        webClient
                .post(port, host, path)
                .sendJsonObject(payload, ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(ar.result().bodyAsJsonObject()));
                    } else {
                        LOGGER.error("No response from adapter.", ar.cause());
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
        return this;
    }

    @Override
    public WebClientService get(int port, String host, String path, Handler<AsyncResult<JsonObject>> resultHandler) {

        webClient
                .get(port, host, path)
                .send(ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(ar.result().bodyAsJsonObject()));
                    } else {
                        LOGGER.error("No response from adapter", ar.cause());
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
        return this;
    }
}
