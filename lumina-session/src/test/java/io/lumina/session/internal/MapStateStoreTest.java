package io.lumina.session.internal;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MapStateStoreTest {
    @Test
    void storesAndRetrievesValues() {
        MapStateStore store = new MapStateStore();

        store.set("name", "Lumina");

        assertThat(store.<String>get("name")).isEqualTo("Lumina");
        assertThat(store.contains("name")).isTrue();
    }

    @Test
    void removesValues() {
        MapStateStore store = new MapStateStore();
        store.set("name", "Lumina");

        store.remove("name");

        assertThat(store.<Object>get("name")).isNull();
        assertThat(store.contains("name")).isFalse();
    }

    @Test
    void computeIfAbsentReturnsSameInstance() {
        MapStateStore store = new MapStateStore();
        List<String> list = store.computeIfAbsent("history", key -> new ArrayList<>());

        list.add("a");

        assertThat(store.<List<String>>get("history")).containsExactly("a");
        assertThat(store.<List<String>>computeIfAbsent("history", key -> new ArrayList<>()))
                .isSameAs(list);
    }
}
