package com.batoni.kvadalert;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DiscordBotListener extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        var interaction = event.getInteraction();
        KvadAlert.getInstance().setChannel(interaction.getChannel());
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        if (!event.getMessage().getAuthor().isBot()) {
            KvadAlert.getInstance().setChannel(event.getChannel());
            KvadAlert.getInstance().setGuild(event.getGuild());
        }
    }
}
