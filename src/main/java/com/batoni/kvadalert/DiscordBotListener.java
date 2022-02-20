package com.batoni.kvadalert;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordBotListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (e.getMessage().getAuthor().isBot()) return;
        switch (e.getMessage().getContentDisplay()) {
            case "!register":
                if (!KvadAlert.getInstance().isChannelRegistered()) {

                    KvadAlert.getInstance().setChannel(e.getGuildChannel());
                    KvadAlert.getInstance().getConfig().set("guild-channel-id", e.getGuildChannel().getId());

                    KvadAlert.getInstance().getConfig().set("registered", true);
                    KvadAlert.getInstance().setChannelRegistered(true);
                    e.getGuildChannel().sendMessage("Channel registered");
                } else e.getGuildChannel().sendMessage("Stop, channel is already registered!");
                break;
            default:
                break;
        }
    }
}
