package com.optimist.pokerstats.pokertracker.player.boundary;


import com.optimist.pokerstats.pokertracker.InMemoryCache;
import com.optimist.pokerstats.pokertracker.player.entity.Player;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.GenericEntity;
import java.util.HashSet;
import java.util.Set;

@Stateless
public class PlayerService {

    @Inject
    InMemoryCache cache;

    public GenericEntity<Set<Player>> findAllAsGenericEntities() {
        return new GenericEntity<Set<Player>>(findAll()) {
        };
    }

    public Set<Player> findAll() {
        return new HashSet<>(cache.getPlayers().values());
    }

    public Player find(Long id) {
        return cache.getPlayers().get(id);
    }

}

