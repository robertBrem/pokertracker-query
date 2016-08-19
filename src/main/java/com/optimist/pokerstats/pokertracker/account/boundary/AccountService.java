package com.optimist.pokerstats.pokertracker.account.boundary;

import com.optimist.pokerstats.pokertracker.InMemoryCache;
import com.optimist.pokerstats.pokertracker.account.entity.AccountPosition;
import com.optimist.pokerstats.pokertracker.player.entity.Player;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.json.*;
import javax.ws.rs.core.GenericEntity;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;

@Stateless
public class AccountService {

    @Inject
    InMemoryCache cache;

    public GenericEntity<Set<AccountPosition>> findAllAsGenericEntities() {
        return new GenericEntity<Set<AccountPosition>>(findAll()) {
        };
    }

    public Set<AccountPosition> findAll() {
        return new HashSet<>(cache.getAccountPositions().values());
    }

    public AccountPosition find(Long id) {
        return cache.getAccountPositions().get(id);
    }

    public GenericEntity<List<AccountPosition>> findByPlayerIdAsGenericEntities(Long playerId) {
        return new GenericEntity<List<AccountPosition>>(findByPlayerId(playerId)) {
        };
    }

    public List<AccountPosition> findByPlayerId(Long playerId) {
        return cache.getAccountPositions().values().stream()
                .filter(ap -> playerId != null && playerId.equals(ap.getPlayerId()))
                .collect(Collectors.toList());
    }

    public JsonArray getHistoryForPlayerAsJson(Long id, Boolean summedUp, TemporalUnit groupUnit) {
        LinkedHashMap<LocalDateTime, Long> history = getHistoryForPlayer(id, summedUp, groupUnit);
        return getJsonArray(history);
    }

    public LinkedHashMap<LocalDateTime, Long> getHistoryForPlayer(Long id, Boolean summedUp, TemporalUnit groupUnit) {
        if (summedUp != null && summedUp) {
            return getSummedUp(getHistory(findByPlayerId(id), groupUnit));
        }
        return getHistory(findByPlayerId(id), groupUnit);
    }

    private JsonArray getJsonArray(LinkedHashMap<LocalDateTime, Long> history) {
        JsonArrayBuilder historyAsJson = Json.createArrayBuilder();
        for (LocalDateTime date : history.keySet()) {
            JsonObjectBuilder entryBuilder = Json.createObjectBuilder();
            entryBuilder.add("date", date.toString());
            if (history.get(date) == null) {
                entryBuilder.add("balance", JsonValue.NULL);
            } else {
                entryBuilder.add("balance", history.get(date));
            }
            JsonObject entry = entryBuilder.build();
            historyAsJson.add(entry);
        }
        return historyAsJson.build();
    }

    public LinkedHashMap<LocalDateTime, Long> getHistory(List<AccountPosition> positions, TemporalUnit groupUnit, Integer maxEntries) {
        LocalDateTime start = positions.stream()
                .map(AccountPosition::getCreationDate)
                .min(LocalDateTime::compareTo)
                .get();
        LocalDateTime end = positions.stream()
                .map(AccountPosition::getCreationDate)
                .max(LocalDateTime::compareTo)
                .get();
        return getHistory(positions, start, end, groupUnit, maxEntries);
    }

    public LinkedHashMap<LocalDateTime, Long> getHistory(List<AccountPosition> positions, TemporalUnit groupUnit) {
        LocalDateTime start = positions.stream()
                .map(AccountPosition::getCreationDate)
                .min(LocalDateTime::compareTo)
                .get();
        LocalDateTime end = positions.stream()
                .map(AccountPosition::getCreationDate)
                .max(LocalDateTime::compareTo)
                .get();
        return getHistory(positions, start, end, groupUnit);
    }

    public LinkedHashMap<LocalDateTime, Long> getHistory(List<AccountPosition> positions, LocalDateTime start, LocalDateTime end, TemporalUnit groupUnit, Integer maxEntries) {
        Map<LocalDateTime, List<AccountPosition>> groupByDate = positions.stream()
                .collect(Collectors.groupingBy(ap -> ap.getRounded(groupUnit)));

        LinkedHashMap<LocalDateTime, Long> history = new LinkedHashMap<>();
        LocalDateTime startRounded = start.truncatedTo(groupUnit);
        LocalDateTime endRounded = end.truncatedTo(groupUnit);
        for (LocalDateTime current = startRounded; !current.isAfter(endRounded); current = current.plus(1L, groupUnit)) {
            LocalDateTime groupUnitRounded = current.truncatedTo(groupUnit);
            List<AccountPosition> positionsForGroupUnit = groupByDate.get(groupUnitRounded);
            if (positionsForGroupUnit == null) {
                positionsForGroupUnit = new ArrayList<>();
            }
            Long balance = positionsForGroupUnit.stream()
                    .map(AccountPosition::getAmount)
                    .reduce(0L, Long::sum);
            history.put(current, balance);
        }

        if (history.size() > maxEntries) {
            Set<LocalDateTime> localDateTimes = history.keySet();
            List<LocalDateTime> list = new ArrayList<>(localDateTimes);
            list.sort(LocalDateTime::compareTo);
            List<LocalDateTime> sublist = list.subList(list.size() - maxEntries, list.size());
            LinkedHashMap<LocalDateTime, Long> historyShortend = new LinkedHashMap<>();
            for (LocalDateTime current : sublist) {
                historyShortend.put(current, history.get(current));
            }
            history = historyShortend;
        }

        return history;
    }

    public LinkedHashMap<LocalDateTime, Long> getHistory(List<AccountPosition> positions, LocalDateTime start, LocalDateTime end, TemporalUnit groupUnit) {
        return getHistory(positions, start, end, groupUnit, Integer.MAX_VALUE);
    }

    public LinkedHashMap<LocalDateTime, Long> getSummedUp(LinkedHashMap<LocalDateTime, Long> history) {
        LinkedHashMap<LocalDateTime, Long> historySummedUp = new LinkedHashMap<>();
        Long currentTotalBalance = 0L;
        for (LocalDateTime date : history.keySet()) {
            Long dateBalance = history.get(date);
            if (dateBalance == null || dateBalance.equals(0L)) {
                historySummedUp.put(date, null);
            } else {
                currentTotalBalance += dateBalance;
                historySummedUp.put(date, currentTotalBalance);
            }
        }
        return historySummedUp;
    }

    public JsonArray getHistoryAsJsonArray(Boolean summedUp, TemporalUnit groupUnit, Integer maxEntries) {
        JsonArrayBuilder result = Json.createArrayBuilder();

        LinkedHashMap<LocalDateTime, Long> allPositions = getHistory(getAllAccountPositions(), groupUnit, maxEntries);
        LocalDateTime start = allPositions.keySet().stream()
                .min(LocalDateTime::compareTo)
                .get();
        LocalDateTime end = allPositions.keySet().stream()
                .max(LocalDateTime::compareTo)
                .get();

        for (Player player : cache.getPlayers().values()) {
            LinkedHashMap<LocalDateTime, Long> history = getHistory(findByPlayerId(player.getId()), start, end, groupUnit);
            if (summedUp != null && summedUp) {
                history = getSummedUp(history);
            }
            JsonObject entry = Json.createObjectBuilder()
                    .add("playerName", player.getFormattedName())
                    .add("history", getJsonArray(history))
                    .build();
            result.add(entry);
        }
        return result.build();
    }

    public GenericEntity<List<AccountPosition>> getAllAccountPositionsAsGenericEntity() {
        List<AccountPosition> positions = getAllAccountPositions();
        return new GenericEntity<List<AccountPosition>>(positions) {
        };
    }

    public List<AccountPosition> getAllAccountPositions() {
        return new ArrayList<>(cache.getAccountPositions().values());
    }
}