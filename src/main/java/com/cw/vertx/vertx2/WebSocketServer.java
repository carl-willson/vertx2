package com.cw.vertx.vertx2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;

import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketServer extends AbstractVerticle {

   public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new WebSocketServer());

  }

  private AtomicInteger clientId = new AtomicInteger();

  @Override
  public void start() throws Exception {

    HttpServer server = vertx.createHttpServer();

    server.webSocketHandler(ws -> {
      ws.handler(data -> {

        System.out.println("Got a data event!! "+ws.scheme());
        ws.writeTextMessage(data.toString(), event -> {
          if (event.succeeded()) {
              System.out.println("Write successful");
            EventBus eventBus = vertx.eventBus();
            eventBus.publish("news.uk.sport", data.toString());
          }
        }
        );
      });

     // vertx.setTimer(10000, l -> ws.writeTextMessage("TIme Out"));

      EventBus eb = vertx.eventBus();


      eb.consumer("news.uk.sport", message -> {
        ws.writeTextMessage(message.body().toString());
        System.out.println("I have received a message: " + message.body());
      });

    } );

    server.requestHandler(req -> {
      handleRequest(req);
    });

    server.listen(15080);
  }



  private void handleRequest(HttpServerRequest req) {
    if (req.uri().equals("/")) {
      req.response().sendFile("ws.html");
    }
  }
}
