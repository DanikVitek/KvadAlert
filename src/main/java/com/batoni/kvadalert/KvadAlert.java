package com.batoni.kvadalert;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.MessageChannel;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.Objects;

public final class KvadAlert extends JavaPlugin implements Listener {

    private GuildMessageChannel channel;

    private JDA api;
    private boolean channelRegistered;

    public void setChannelRegistered(boolean channelRegistered) {
        this.channelRegistered = channelRegistered;
    }

    public void setApi(JDA api) {
        this.api = api;
    }

    public void setChannel(GuildMessageChannel channel) {
        this.channel = channel;
    }

    public static @NotNull KvadAlert getInstance() {
        return JavaPlugin.getPlugin(KvadAlert.class);
    }

    public boolean isChannelRegistered() {
        return channelRegistered;
    }

    @Override
    public void onEnable() {
        getConfig().options().copyDefaults();
        saveDefaultConfig();
        setChannelRegistered(getConfig().getBoolean("registered"));
        Bukkit.getPluginManager().registerEvents(this, this);

        Objects.requireNonNull(getCommand("registerdiscord")).setExecutor(new RegisterDiscordCommand());

        if (isChannelRegistered()) {
            try {
                api = JDABuilder
                        .create(getConfig().getString("bot-token"), GatewayIntent.GUILD_MESSAGES)
                        .addEventListeners(new DiscordBotListener())
                        .build();
                channel = (GuildMessageChannel) api.getGuildChannelById(
                        Objects.requireNonNull(getConfig().getString("guild-channel-id"))
                );
            } catch (LoginException e) {
                e.printStackTrace();
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(@NotNull PlayerJoinEvent e) {
        if (isChannelRegistered()) {
            try {
                var playerFace = requestPlayerFace(e.getPlayer());
                String playerName = e.getPlayer().getName();
                channel.sendMessage(playerName).addFile(playerFace.toByteArray(), playerName + ".png");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private @NotNull ByteArrayOutputStream requestPlayerFace(@NotNull Player player) throws IOException {
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
        return headImageByteOutputStream;
    }

    private static JsonElement parseBase64(String base64) {
        return JsonParser.parseString(new String(Base64.getDecoder().decode(base64)));
    }
}
