package com.optimist.pokerstats.pokertracker.account.event;

import com.optimist.pokerstats.pokertracker.CoreEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AccountPositionEvent implements CoreEvent {
    private final Long id;
}
