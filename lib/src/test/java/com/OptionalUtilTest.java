package com;

import org.junit.jupiter.api.Test;

import com.utils.optional.OptionalUtil;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OptionalUtilTest {

    @Test
    void firstPresent_returnsEmpty_whenAllEmpty() {
        assertTrue(OptionalUtil.firstPresent(Optional.empty(), Optional.empty()).isEmpty());
    }

    @Test
    void firstPresent_returnsFirst_whenExactlyOnePresent() {
        Optional<String> v = Optional.of("one");
        assertEquals("one", OptionalUtil.firstPresent(v, Optional.empty()).get());
    }

    @Test
    void firstPresent_returnsFirstPresent_whenMultiplePresent() {
        Optional<String> a = Optional.of("A");
        Optional<String> b = Optional.of("B");
        assertEquals("A", OptionalUtil.firstPresent(a, b, Optional.empty()).get());
    }
}
