package com.cw.vertx.vertx2;

import io.vertx.core.*;

import java.util.concurrent.atomic.AtomicInteger;


public class MainVerticle extends AbstractVerticle {

  private String instanceId;

  private static AtomicInteger inst = new AtomicInteger(1);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    instanceId = "Instance "+inst.getAndIncrement();
    System.out.println("Instance ["+getName() +"] stsrt called");
    VertxLockUtil vertxLockUtil = new VertxLockUtil(vertx);

    vertxLockUtil.executeOnceOnly("MainVerticle Startup", getName(), this::doSpecialStartupCode)
      .onFailure(t -> {
        System.out.println("Instance ["+getName() +"] only once failed ! ["+t.getMessage()+"]");
      })
    .onComplete( v -> {
      System.out.println("Instance ["+getName() +"] only once completed ["+v.result()+"]");
      vertx.createHttpServer().requestHandler(req -> {
        req.response()
          .putHeader("content-type", "text/plain")
          .end("Hello from Vert.x!");
      }).listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println(getName()+ " HTTP server started on port 8888");
          System.out.println("Verticle " + deploymentID() + " / " + instanceId + " has started");
        } else {
          startPromise.fail(http.cause());
        }
      });


    });
  }

  private String getName () {
    return deploymentID() + " / "+instanceId;
  }

  private Future<String> doSpecialStartupCode(String input) {

    Promise<String>  p = Promise.promise();
    System.out.println("Doing special startup code using ["+getName() +"] !!");

    vertx.setTimer(2000, tid -> {
        p.complete("Special startup done");
    });

    return p.future();
  }

  public static void main(String[] args) {

    MainVerticle.go();
//    MainVerticle webBasedVerticle = new MainVerticle();
//    webBasedVerticle.go();

  }

  private static void go() {
    VertxOptions options = new VertxOptions();
        options.setBlockedThreadCheckInterval(40000000);
    Vertx vertx = Vertx.vertx(options);

    DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(5);

    vertx.deployVerticle(MainVerticle.class, deploymentOptions)
      .onComplete(resString -> {
        System.out.println("All instances have completed start up");
      });


  }

}
