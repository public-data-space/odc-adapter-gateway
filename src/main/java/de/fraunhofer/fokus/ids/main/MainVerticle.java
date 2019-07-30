package de.fraunhofer.fokus.ids.main;

import de.fraunhofer.fokus.ids.services.webclient.WebClientService;
import de.fraunhofer.fokus.ids.services.webclient.WebClientServiceVerticle;
import io.vertx.core.*;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import org.apache.http.entity.ContentType;

import java.util.*;

public class MainVerticle extends AbstractVerticle {
    private static Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class.getName());
    private Router router;
    private WebClientService webClientService;

    @Override
    public void start(Future<Void> startFuture) {
        this.router = Router.router(vertx);

        DeploymentOptions deploymentOptions = new DeploymentOptions();
        deploymentOptions.setWorker(true);


        vertx.deployVerticle(WebClientServiceVerticle.class.getName(), deploymentOptions, reply -> {
            if(reply.succeeded()){
                LOGGER.info("WebClientService started");
                this.webClientService = WebClientService.createProxy(vertx, "de.fraunhofer.fokus.ids.webServiceClient");
            }
        });
        createHttpServer();
    }

    private void createHttpServer() {
        HttpServer server = vertx.createHttpServer();

        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("X-PINGARUNER");

        Set<HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(HttpMethod.GET);
        allowedMethods.add(HttpMethod.POST);

        router.route().handler(CorsHandler.create("*").allowedHeaders(allowedHeaders).allowedMethods(allowedMethods));
        router.route().handler(BodyHandler.create());

        router.post("/create/:name").handler(routingContext ->
                    create(routingContext.request().getParam("name"), routingContext.getBodyAsJson(), reply -> reply(reply, routingContext.response())));

        router.route("/delete/:name/:id").handler(routingContext ->
                delete(routingContext.request().getParam("name"), Long.parseLong(routingContext.request().getParam("id")), reply -> reply(reply, routingContext.response())));

        router.post("/getFile/:name").handler(routingContext ->
                getFile(routingContext.request().getParam("name"), routingContext.getBodyAsJson(), reply -> reply(reply, routingContext.response())));

        router.route("/supported/:name").handler(routingContext ->
                supported(routingContext.request().getParam("name"), reply -> reply(reply, routingContext.response())));

        LOGGER.info("Starting Adapter Gateway...");
        server.requestHandler(router).listen(8093);
        LOGGER.info("Adapter Gateway successfully started.");
    }

    private void supported(String name, Handler<AsyncResult<JsonObject>> resultHandler){
        webClientService.get(8094, "localhost","/getAdapter/"+name, reply -> {
            if(reply.succeeded()){
                webClientService.get(reply.result().getInteger("port"), reply.result().getString("host"), "/supported/", reply2 -> {
                    if(reply2.succeeded()){
                        resultHandler.handle(Future.succeededFuture(reply2.result()));
                    }
                    else{
                        LOGGER.info("Create failed in adapter",reply2.cause());
                        resultHandler.handle(Future.failedFuture(reply2.cause()));
                    }
                });
            }
            else{
                LOGGER.info("Adapter could not be retrieved.",reply.cause());
            }
        });
    }

    private void getFile(String name, JsonObject jsonObject, Handler<AsyncResult<JsonObject>> resultHandler){
        webClientService.get(8094, "localhost","/getAdapter/"+name, reply -> {
            if(reply.succeeded()){
                webClientService.post(reply.result().getInteger("port"), reply.result().getString("host"), "/getFile", jsonObject, reply2 -> {
                    if(reply2.succeeded()){
                        resultHandler.handle(Future.succeededFuture(reply2.result()));
                    }
                    else{
                        LOGGER.info("Create failed in adapter",reply2.cause());
                        resultHandler.handle(Future.failedFuture(reply2.cause()));
                    }
                });
            }
            else{
                LOGGER.info("Adapter could not be retrieved.",reply.cause());
            }
        });
    }

    private void delete(String name, Long id, Handler<AsyncResult<JsonObject>> resultHandler){
        webClientService.get(8094, "localhost","/getAdapter/"+name, reply -> {
            if(reply.succeeded()){
                webClientService.get(reply.result().getInteger("port"), reply.result().getString("host"), "/create/"+id, reply2 -> {
                    if(reply2.succeeded()){
                        resultHandler.handle(Future.succeededFuture(reply2.result()));
                    }
                    else{
                        LOGGER.info("Create failed in adapter",reply2.cause());
                        resultHandler.handle(Future.failedFuture(reply2.cause()));
                    }
                });
            }
            else{
                LOGGER.info("Adapter could not be retrieved.",reply.cause());
            }
        });
    }

    private void create(String name, JsonObject request, Handler<AsyncResult<JsonObject>> resultHandler){
        webClientService.get(8094, "localhost","/getAdapter/"+name, reply -> {
           if(reply.succeeded()){
                webClientService.post(reply.result().getInteger("port"), reply.result().getString("host"), "/create", request, reply2 -> {
                    if(reply2.succeeded()){
                        resultHandler.handle(Future.succeededFuture(reply2.result()));
                    }
                    else{
                        LOGGER.info("Create failed in adapter",reply2.cause());
                        resultHandler.handle(Future.failedFuture(reply2.cause()));
                    }
                });
           }
           else{
               LOGGER.info("Adapter could not be retrieved.",reply.cause());
           }
        });
    }

    private void reply(AsyncResult result, HttpServerResponse response) {
        if (result.succeeded()) {
            if (result.result() != null) {
                String entity = result.result().toString();
                response.putHeader("content-type", ContentType.APPLICATION_JSON.toString());
                response.end(entity);
            } else {
                response.setStatusCode(404).end();
            }
        } else {
            response.setStatusCode(404).end();
        }
    }

    public static void main(String[] args) {
        String[] params = Arrays.copyOf(args, args.length + 1);
        params[params.length - 1] = MainVerticle.class.getName();
        Launcher.executeCommand("run", params);
    }
}