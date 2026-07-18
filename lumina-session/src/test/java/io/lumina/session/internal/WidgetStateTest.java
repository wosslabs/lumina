package io.lumina.session.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WidgetStateTest {
    @Test
    void valuePersistsUntilReplaced() {
        WidgetState widgets = new WidgetState();

        widgets.set("name", "Ada");

        assertThat(widgets.<String>value("name")).isEqualTo("Ada");
        assertThat(widgets.<String>value("name")).isEqualTo("Ada");
    }

    @Test
    void clickIsConsumedOncePerRun() {
        WidgetState widgets = new WidgetState();
        widgets.set("btn", Boolean.TRUE);

        assertThat(widgets.consumeClick("btn")).isTrue();
        assertThat(widgets.consumeClick("btn")).isFalse();
    }

    @Test
    void nonClickValueDoesNotProduceClick() {
        WidgetState widgets = new WidgetState();
        widgets.set("btn", "true");

        assertThat(widgets.consumeClick("btn")).isFalse();
    }

    @Test
    void chatSubmitIsConsumedOncePerRun() {
        WidgetState widgets = new WidgetState();
        widgets.setChatSubmit("chat", "hello");

        assertThat(widgets.consumeChatSubmit("chat")).isEqualTo("hello");
        assertThat(widgets.consumeChatSubmit("chat")).isNull();
    }

    @Test
    void nonStringValueDoesNotProduceChatSubmit() {
        WidgetState widgets = new WidgetState();
        widgets.set("chat", Boolean.TRUE);

        assertThat(widgets.consumeChatSubmit("chat")).isNull();
    }
}
