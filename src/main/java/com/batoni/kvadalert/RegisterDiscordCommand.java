package com.batoni.kvadalert;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.login.LoginException;
import java.util.List;

public class RegisterDiscordCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        var TOKEN = KvadAlert.getInstance().getConfig().getString("bot-token");
        if(KvadAlert.getInstance().isChannelRegistered()){
            sender.sendMessage("The channel is already registered!");
            return true;
        }

        try {
            var api = JDABuilder
                    .create(TOKEN, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordBotListener())
                    .build();
            KvadAlert.getInstance().setApi(api);
        } catch (LoginException e) {e.printStackTrace();}

        return true;
    }

}
