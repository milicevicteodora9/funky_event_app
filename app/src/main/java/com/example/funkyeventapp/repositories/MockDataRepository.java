package com.example.funkyeventapp.repositories;

import com.example.funkyeventapp.models.Event;
import com.example.funkyeventapp.models.EventStatus;
import com.example.funkyeventapp.models.EventType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockDataRepository {
    private final List<Event> events = Arrays.asList(
            new Event("1", "Addiko Banka - Branding", EventType.CAMPAIGN, "20 Aug 2026", "27 Aug 2026", "Beograd", "Addiko Banka", EventStatus.CURRENT, 0),
            new Event("2", "Flying Tiger Store Opening", EventType.EVENT, "3 Sep 2026", "", "Galerija, Beograd", "Flying Tiger Copenhagen", EventStatus.CURRENT, 0),
            new Event("3", "Funky Summer Activation", EventType.EVENT, "12 Sep 2026", "13 Sep 2026", "Novi Sad", "Funky Business", EventStatus.CURRENT, 0),
            new Event("4", "Addiko Bank Promo Weekend", EventType.CAMPAIGN, "5 Jul 2026", "6 Jul 2026", "Niš", "Addiko Banka", EventStatus.COMPLETED, 0),
            new Event("5", "Flying Tiger Spring Launch", EventType.EVENT, "18 May 2026", "", "Beograd", "Flying Tiger Copenhagen", EventStatus.COMPLETED, 0)
    );

    public List<Event> getEvents(EventStatus status) {
        List<Event> result = new ArrayList<>();
        for (Event event : events) if (event.getStatus() == status) result.add(event);
        return result;
    }
}
