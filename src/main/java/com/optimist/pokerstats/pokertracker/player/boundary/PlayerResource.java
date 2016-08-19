package com.optimist.pokerstats.pokertracker.player.boundary;


import com.airhacks.porcupine.execution.boundary.Dedicated;
import com.optimist.pokerstats.pokertracker.account.boundary.AccountService;
import com.optimist.pokerstats.pokertracker.account.entity.AccountPosition;

import javax.annotation.security.PermitAll;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.*;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@PermitAll
@Stateless
@Path("players")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PlayerResource {

    @Inject
    PlayerService service;

    @Inject
    AccountService accountPositionService;

    @Inject
    @Dedicated
    ExecutorService threadPool;

    @GET
    public void getAll(@Suspended AsyncResponse response) {
        CompletableFuture
                .supplyAsync(service::findAllAsGenericEntities, threadPool)
                .thenAccept(response::resume);
    }

    @GET
    @Path("{id}")
    public void find(@Suspended AsyncResponse response, @PathParam("id") Long id) {
        response.resume(service.find(id));
    }

    @GET
    @Path("{id}/accountpositions")
    public void getAccountPositionsForPlayer(@Suspended AsyncResponse response, @PathParam("id") Long id) {
        GenericEntity<List<AccountPosition>> positions = accountPositionService.findByPlayerIdAsGenericEntities(id);
        response.resume(positions);
    }

    @GET
    @Path("{id}/accounthistory")
    public void getAccountHistoryForPlayer(@Suspended AsyncResponse response, @PathParam("id") Long id, @QueryParam("summedUp") Boolean summedUp) {
        response.resume(accountPositionService.getHistoryForPlayerAsJson(id, summedUp, ChronoUnit.MINUTES));
    }

    @GET
    @Path("accounthistory")
    public void getAccountHistory(@Suspended AsyncResponse response, @QueryParam("summedUp") Boolean summedUp, @QueryParam("timeUnit") String timeUnitText, @QueryParam("maxEntries") Integer maxEntries) {
        ChronoUnit timeUnit = ChronoUnit.MINUTES;
        if (timeUnitText != null && !timeUnitText.isEmpty()) {
            timeUnit = ChronoUnit.valueOf(timeUnitText);
        }
        Integer max = Integer.MAX_VALUE;
        if (maxEntries != null) {
            max = maxEntries;
        }
        response.resume(accountPositionService.getHistoryAsJsonArray(summedUp, timeUnit, max));
    }

    @GET
    @Path("{id}/balance")
    public void getBalance(@Suspended AsyncResponse response, @PathParam("id") Long id) {
        List<AccountPosition> positions = accountPositionService.findByPlayerId(id);
        Long balance = positions.stream()
                .map(AccountPosition::getAmount)
                .reduce(0L, Long::sum);
        JsonObject balanceJson = Json.createObjectBuilder()
                .add("value", balance)
                .add("currency", "CHF")
                .build();
        response.resume(balanceJson);
    }

}

