package com.cw.vertx.vertx2;

import io.vertx.core.*;
import io.vertx.core.http.Cookie;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthenticationHandler;
import io.vertx.ext.web.handler.BasicAuthHandler;

import java.util.Base64;

public class WebBasedVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);

//    router.route().handler(ctx -> {
//        HttpServerResponse response = ctx.response();
//        response.setChunked(true);
//
//        response.write("a chunk for all routes\n");
//        //ctx.next();
//      }
//      );


    Route route1 = router.route("/api/test/all");
    route1.handler(ctx -> {

      handleALL(ctx);
    });

    Route route = router.route("/some/path/");
    route.handler(ctx -> {

      HttpServerResponse response = ctx.response();
      // enable chunked responses because we will be adding data as
      // we execute over other handlers. This is only required once and
      // only if several handlers do output.
      if (!response.isChunked() ) {
        response.setChunked(true);
      }

      response.write("route1\n");

      // Call the next matching route after a 5 second delay
      ctx.vertx().setTimer(1000, tid -> ctx.next());
    });

    route.handler(ctx -> {

      HttpServerResponse response = ctx.response();
      response.write("route2\n");

      // Call the next matching route after a 5 second delay
      ctx.vertx().setTimer(5000, tid -> ctx.next());
    });

    route.handler(ctx -> {

      HttpServerResponse response = ctx.response();
      response.write("route3");

      // Now end the response
      ctx.response().end();
    });

    AuthenticationProvider ap = new AuthenticationProvider() {
      @Override
      public void authenticate(JsonObject jsonObject, Handler<AsyncResult<User>> handler) {
        System.out.println("Authentiing!");
        JsonObject authInfo = new JsonObject()
          .put("username", "tim").put("password", "mypassword");
        User user = User.create(authInfo);
        //User u = new UserImpl();

        AsyncResult<User> ar = new AsyncResult<User>() {
          @Override
          public User result() {
            return user;
          }

          @Override
          public Throwable cause() {
            return null;
          }

          @Override
          public boolean succeeded() {
            return true;
          }

          @Override
          public boolean failed() {
            return false;
          }
        };

        handler.handle(ar);
      }
    } ;

    AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(ap);

// All requests to paths starting with '/private/' will be protected
    router.route("/private/*").handler(basicAuthHandler);

    router.route("/someotherpath").handler(ctx -> {

      // This will be public access - no login required

    });

    router.route("/private/somepath").handler(ctx -> {

      // This will require a login

      // This will have the value true
      boolean isAuthenticated = ctx.user() != null;

     JWTAuth provider = JWTAuth.create(vertx, new JWTAuthOptions()
        .addPubSecKey(new PubSecKeyOptions()
          .setAlgorithm("HS256")
          .setBuffer("keyboard cat")));

      JWTOptions options = new JWTOptions();
      options.setExpiresInSeconds(30);

      String claims = """
                {
                    'u' : '05970490',
                    'perms' : [
                    '7:1,4,25,200',
                    '41465:1,2,25,26,205'
                    ]
                 }
        """.replace("'", "\"");


      String token = provider.generateToken(new JsonObject(claims), options);

      System.out.println("Token "+token+" bytes "+token.getBytes().length);
      String[] tokenParts = token.split("\\.");

      byte[] decodedBytes = Base64.getDecoder().decode(tokenParts[0]);
      String decodedString = new String(decodedBytes);
      System.out.println("Token part ["+decodedString+"]");

      decodedBytes = Base64.getDecoder().decode(tokenParts[1]);
      decodedString = new String(decodedBytes);
      System.out.println("Token part ["+decodedString+"]");

      vertx.setTimer(500, tm -> {
        Future<User> fUser = provider.authenticate(new JsonObject("{ \"token\": \"" + token + "\" }"));

        fUser.onSuccess(user -> {
          ctx.response().putHeader("MyJWT", token);
          Cookie c = Cookie.cookie("MyFancyToken",token);
          ctx.response().addCookie(c);
          ctx.end("We did it okay!! " + token);
        });
        fUser.onFailure(t -> {
            t.printStackTrace();
            ctx.end("Bad news - errory!! " + token);
          }
        );

      });



    });

    server.requestHandler(router).listen(8888);

    startPromise.complete();
  }

  private void handleALL(RoutingContext ctx) {
    HttpServerResponse response = ctx.response();
    response.write("Hello there!");

    // Now end the response
    ctx.response().end();
  }


  public static void main(String[] args) {
    WebBasedVerticle webBasedVerticle = new WebBasedVerticle();

    webBasedVerticle.go();

  }

  private void go() {
    VertxOptions options = new VertxOptions();
    options.setBlockedThreadCheckInterval(40000000);
    Vertx vertx = Vertx.vertx(options);

    vertx.deployVerticle(this);

  }
}
