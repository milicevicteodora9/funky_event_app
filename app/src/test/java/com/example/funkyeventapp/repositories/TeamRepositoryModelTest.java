package com.example.funkyeventapp.repositories;

import static org.junit.Assert.assertEquals;

import com.example.funkyeventapp.models.TeamMember;

import org.junit.Test;

public class TeamRepositoryModelTest {
    @Test public void fullNameIsStoredAsFirstAndLastName() {
        TeamMember member = new TeamMember("member", "Ana Marija Petrović", "", "",
                "", "", "", true);

        assertEquals("Ana Marija", member.getFirstName());
        assertEquals("Petrović", member.getLastName());
        assertEquals("Ana Marija Petrović", member.getFullName());
    }

}
