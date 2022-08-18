package com.cw.vertx.vertx2;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.shareddata.Lock;
import io.vertx.core.shareddata.SharedData;

import java.util.function.Function;

public class VertxLockUtil {

  private final Vertx vertx;


  public Future<String> executeOnceOnly (String actionName, String instanceName, Function<String, Future<String>> codeToExecute) {
    Promise<String> promise = Promise.promise();

    SharedData sharedData = vertx.sharedData();

    sharedData.getLocalLock(actionName, res -> {
      if (res.succeeded()) {
        // Got the lock!
        Lock lock = res.result();

        System.out.println("Instance ["+instanceName+"] got lock!");

        LocalMap<String, Boolean> localMap  = sharedData.getLocalMap("OnlyOnceActions");

        boolean done = localMap.getOrDefault(actionName, false);

        if (!done) {
          localMap.put(actionName, true);

           Future<String> fResult = codeToExecute.apply(actionName)
               .onSuccess( s -> {
               })
                 .onFailure( t -> {
                   promise.fail(new RuntimeException(t));
                 })
                   .onComplete( s -> {
                     lock.release();
                   });
          promise.complete("Code submitted");

        } else {
          System.out.println("Instance ["+instanceName+"] already done once - skipping!");
          lock.release();
          promise.complete("Skipped");
        }


          // 5 seconds later we release the lock so someone else can get it





      } else {
        promise.fail(new RuntimeException("Could not obtain lock "+actionName));
      }
    });

    return promise.future();
  }

  public VertxLockUtil(Vertx vertx) {
    this.vertx = vertx;
  }
}
