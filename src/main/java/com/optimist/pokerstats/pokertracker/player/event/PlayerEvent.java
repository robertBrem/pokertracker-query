package com.optimist.pokerstats.pokertracker.player.event;

import com.optimist.pokerstats.pokertracker.CoreEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class PlayerEvent implements CoreEvent {
    private final Long id;
}
