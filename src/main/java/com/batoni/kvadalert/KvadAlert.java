package com.batoni.kvadalert;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;

public final class KvadAlert extends JavaPlugin implements Listener {

    private MessageChannel channel;
    private Guild guild;

    public static @NotNull KvadAlert getInstance() {
        return JavaPlugin.getPlugin(KvadAlert.class);
    }

    public void setChannel(MessageChannel channel) {
        this.channel = channel;
    }

    public void setGuild(Guild guild) {
        this.guild = guild;
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        Bukkit.getPluginManager().registerEvents(this, this);

        final var TOKEN = getConfig().getString("bot-token");
        final var TEST_GUILD_ID = getConfig().getString("test-server-id");

        try {
            JDA api = JDABuilder
                    .create(TOKEN, GatewayIntent.GUILD_MESSAGES)
                    .addEventListeners(new DiscordBotListener())
                    .build();
        } catch (LoginException e) {
            e.printStackTrace();
        }

//        var commandData = Commands.slash("register", "Register a channel to log players");
//        guild.upsertCommand(commandData)
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        try {
            Player player = e.getPlayer();
            Connection.Response response = Jsoup
                    .connect("https://sessionserver.mojang.com/session/minecraft/profile/" + player.getUniqueId())
                    .method(Connection.Method.GET)
                    .ignoreContentType(true)
                    .ignoreHttpErrors(true)
                    .execute();
            var responseObject = JsonParser.parseString(response.body()).getAsJsonObject();
            var valueObject = parseBase64(
                    responseObject
                            .getAsJsonArray("properties")
                            .get(0)
                            .getAsJsonObject()
                            .get("value")
                            .getAsString()
            ).getAsJsonObject();
            var skinURL = new URL(valueObject.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString());
            var skinImage = ImageIO.read(skinURL);
            var headImage = switch (skinImage.getHeight()) {
                case 32 -> skinImage.getSubimage(8, 8, 8, 8);
                case 64 -> {
                    var layer0Image = skinImage.getSubimage(8, 8, 8, 8);
                    var layer0Graphics = layer0Image.getGraphics();
                    var layer1Image = skinImage.getSubimage(40, 8, 8, 8);
                    layer0Graphics.drawImage(layer1Image, 0, 0, null);
                    layer0Graphics.dispose();
                    yield layer0Image;
                }
                default -> throw new IllegalStateException("Unexpected skin height value: " + skinImage.getHeight());
            };
            var headImageByteOutputStream = new ByteArrayOutputStream();
            ImageIO.write(headImage, "PNG", headImageByteOutputStream);
            String playerName = player.getName();
            channel.sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setTitle("Player Name")
                                    .addField(new MessageEmbed.Field("Player Name", playerName, true))
                                    .setColor(Color.RED)
                                    .build()
                    )
                    .addFile(headImageByteOutputStream.toByteArray(), playerName + ".png");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static JsonElement parseBase64(String base64) {
        return JsonParser.parseString(new String(Base64.getDecoder().decode(base64)));
    }
}
