package com.cw.vertx.vertx2;

import io.vertx.core.*;
import io.vertx.ext.web.client.WebClient;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;


public class PerfornanceTestVerticle extends AbstractVerticle {

  private WebClient webClient;
  private String instanceId;
  private String delayStr;
  private boolean time = false;
  private long executionSuccessCount;
  private long executionFailCount;
  private long executionLast10Secs;
  private long totalTimeTakenSuccess;
  private long totalTimeTakenFail;
  private long workerTimerId;
  private boolean finished=false;

  private static AtomicInteger inst = new AtomicInteger(1);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {


    long instId =inst.getAndIncrement();

    instanceId = "Instance "+instId;
    long delay =instId*10;
    delayStr = String.valueOf(delay);

    System.out.println("Instance ["+getName() +"] start called");

    webClient = WebClient.create(vertx);

    vertx.eventBus().consumer("COMPLETE", m -> {
      finished = true;
      vertx.setTimer(2000, tid -> {
        printResults();
      });
    });

    vertx.eventBus().consumer("START TIMING", m -> {
      time = true;
    });

    VertxLockUtil vertxLockUtil = new VertxLockUtil(vertx);

    vertxLockUtil.executeOnceOnly("MainVerticle Startup", getName(), this::doSpecialStartupCode)
      .onFailure(t -> {
        System.out.println("Instance ["+getName() +"] only once failed ! ["+t.getMessage()+"]");
      })
    .onComplete( v -> {
      System.out.println("Instance ["+getName() +"] starting..... ["+v.result()+"]");
      getVertx().setTimer(2000 , tid -> {
          long l = System.currentTimeMillis()  ;
          doRequest(l);

      });
      startPromise.complete();
    });
  }

  private void printResults () {
    vertx.cancelTimer(workerTimerId);

    double ave = totalTimeTakenSuccess / executionSuccessCount;

    System.out.println(" Results ["+getName()+"] Executions ["+executionSuccessCount+"] Ave time ["+ave+"] millis, "
      +totalTimeTakenSuccess+", exes: "+executionSuccessCount);
  }


  private void doRequest(long lastTime) {

    // do action
    long startTime = System.currentTimeMillis();

// Send a GET request
    webClient
      .get(8888, "localhost", "/some-uri")
      .putHeader("Delay", delayStr)
      .send()
      .onSuccess(response -> {
        long timeTaken = System.currentTimeMillis() - startTime;
        System.out.println(LocalDateTime.now()+", Instance ["+getName() +"] Rqs done, code " + response.statusCode()+" in "+timeTaken+" millis" );
  //      if ( executionSuccessCount==5 ) {
  //        totalTimeTakenSuccess += (timeTaken*5);

          if ( time) {
            totalTimeTakenSuccess += timeTaken;
            executionSuccessCount++;
          }
      })
      .onFailure(err -> {
        long timeTaken = System.currentTimeMillis() - startTime;
        System.out.println("Something went wrong " + err.getMessage());
        totalTimeTakenFail+=timeTaken;
        executionFailCount++;
      })
      .onComplete( v -> {

        scheduleNext(lastTime);
      });
  }

  private void scheduleNext(long lastTime) {

    if (finished) {
      return;
    }

    long curTime = System.currentTimeMillis();
    long nextTime =  (lastTime + 1000) - curTime;

 //   Instant now = Instant.now();

  //  Instant next = now.plusSeconds(1);

 //   System.out.println("last: "+lastTime+ ", cur: "+curTime+ ", next: "+nextTime);
    if ( nextTime<=0 ) {
      System.out.println("Warning timer isnt keeping up");
      nextTime =1;
    }

    long nextBaseline=curTime+nextTime;

    workerTimerId = getVertx().setTimer(nextTime, tid -> {
         doRequest(nextBaseline);
    });
  }

  private String getName () {
    return deploymentID() + " / "+instanceId;
  }

  private Future<String> doSpecialStartupCode(String input) {
    return Future.succeededFuture("Nothing special to do");
  }

  @Override
  public void stop() throws Exception {
    super.stop();
    try {
      webClient.close();
    } catch (Exception e) {

    }
  }

  public static void main(String[] args) {

    PerfornanceTestVerticle.go();
//    MainVerticle webBasedVerticle = new MainVerticle();
//    webBasedVerticle.go();

  }

  private static void go() {
    VertxOptions options = new VertxOptions();
        options.setBlockedThreadCheckInterval(40000000);
    Vertx vertx = Vertx.vertx(options);

    DeploymentOptions deploymentOptions = new DeploymentOptions().setInstances(1000);

    vertx.deployVerticle(PerfornanceTestVerticle.class, deploymentOptions)

      .onComplete(deployFuture -> {
        String deployId = deployFuture.result();
        System.out.println(deployId +" All instances have completed start up");

        vertx.setTimer(5000, tid -> {
          System.out.println("---- Start Timing ----");
          vertx.eventBus().publish("START TIMING", "GO GO GO");

          vertx.setTimer(20000, tid2 -> {
            System.out.println("---- Stopping Test ----");
            vertx.eventBus().publish("COMPLETE", "TIMES UP");
            vertx.setTimer(5000, tid1 -> {
              vertx.undeploy(deployId);
            } );
          });

        });



      });


  }

}
