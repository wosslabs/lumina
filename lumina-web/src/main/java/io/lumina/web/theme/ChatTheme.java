package io.lumina.web.theme;

import io.lumina.plugin.ThemeSpi;

/** Built-in chat layout theme stylesheet. */
public final class ChatTheme implements ThemeSpi {
    @Override
    public String cssResource() {
        return "/lumina-web/themes/chat.css";
    }
}
