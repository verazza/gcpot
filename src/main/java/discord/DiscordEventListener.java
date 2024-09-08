package discord;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.slf4j.Logger;

import com.google.inject.Inject;

import common.Config;
import gcp.InstanceManager;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class DiscordEventListener extends ListenerAdapter {

	public static String PlayerChatMessageId = null;
	
	private final Logger logger;
	private final String gcpToken, responseUrl;
	private final Long gcpChannelId;
	private final boolean require;
	private final InstanceManager gcp;

	@Inject
	public DiscordEventListener (Logger logger, Config config, InstanceManager gcp) {
		this.logger = logger;
		this.gcp = gcp;
		this.gcpToken = config.getString("Discord.Token", "");
		this.responseUrl = config.getString("Discord.ResponseUrl", "");
		this.gcpChannelId = config.getLong("Discord.GCPChannelId", 0);
		this.require = gcpToken != null && !gcpToken.isEmpty() && 
				responseUrl != null && !responseUrl.isEmpty() && 
				gcpChannelId != 0;
	}
	
	@SuppressWarnings("null")
	@Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent e) {
		User user = e.getUser();
		String slashCmd = e.getName();
		MessageChannel channel = e.getChannel();
		String channelId = channel.getId(),
			guildId = e.getGuild().getId();

		String channelLink = String.format("https://discord.com/channels/%s/%s", guildId, gcpChannelId);

		switch (slashCmd) {
			case "gcp-start" -> {
				String userMention = user.getAsMention();
				ReplyCallbackAction messageAction;

				if (!require) {
					messageAction = e.reply("コンフィグの設定が不十分なため、コマンドを実行できません。").setEphemeral(true);
					messageAction.queue();
					return;
				}

				// 上でgcpChannelIdが0でない(=未定義)なら
				String gcpChannelId2 = Long.toString(gcpChannelId);
				if (!channelId.equals(gcpChannelId2)) {
					messageAction = e.reply("GCPのコマンドは " + channelLink + " で実行してください。").setEphemeral(true);
					messageAction.queue();
					return;
				}

				if (gcp.isInstanceRunning()) {
					if (gcp.isInstanceFrozen()) {
						messageAction = e.reply("Terrariaサーバーは既にオンラインです！").setEphemeral(true);
						messageAction.queue();
					} else {
						
					}

					messageAction = e.reply("Terrariaサーバーは既にオンラインです！").setEphemeral(true);
					messageAction.queue();
				} else {
					try {
						ProcessBuilder gcpprocessBuilder = new ProcessBuilder(gcpExecFilePath);
						gcpprocessBuilder.start();
						messageAction = e.reply(userMention + " Terrariaサーバーを起動させました。\nまもなく起動します。").setEphemeral(false);
						messageAction.queue();
					} catch (IOException e1) {
						messageAction = e.reply(userMention + " 内部エラーが発生しました。\nサーバーが起動できません。").setEphemeral(false);
						messageAction.queue();
						logger.error("An IOException error occurred: " + e1.getMessage());
						for (StackTraceElement element : e1.getStackTrace()) {
							logger.error(element.toString());
						}
					}
				}
			}
			case "gcp-stop" -> {
				String userMention = user.getAsMention();
				ReplyCallbackAction messageAction;
				if (!require) {
					messageAction = e.reply("コンフィグの設定が不十分なため、コマンドを実行できません。").setEphemeral(true);
					messageAction.queue();
					return;
				}

				String gcpChannelId2 = Long.toString(gcpChannelId);
				if (!channelId.equals(gcpChannelId2)) {
					messageAction = e.reply("テラリアのコマンドは " + channelLink + " で実行してください。").setEphemeral(true);
					messageAction.queue();
					return;
				}

				if (isgcp()) {
					try {
						String urlString = "http://" + gcpHost + ":" + gcpPort + "/v2/server/off?token=" + gcpToken + "&confirm=true&nosave=false";

						URI uri = new URI(urlString);
						URL url = uri.toURL();
						
						HttpURLConnection con = (HttpURLConnection) url.openConnection();
						con.setRequestMethod("GET");
						con.setRequestProperty("Content-Type", "application/json; utf-8");

						int code = con.getResponseCode();
						switch (code) {
							case 200 -> {
								messageAction = e.reply(userMention + " Terrariaサーバーを正常に停止させました。").setEphemeral(false);
								messageAction.queue();
							}
							default -> {
								messageAction = e.reply(userMention + " 内部エラーが発生しました。\nサーバーが正常に停止できなかった可能性があります。").setEphemeral(false);
								messageAction.queue();
							}
						}
						
					} catch (IOException | URISyntaxException e2) {
						logger.error("An IOException | URISyntaxException error occurred: " + e2.getMessage());
						for (StackTraceElement element : e2.getStackTrace()) {
							logger.error(element.toString());
						}

						messageAction = e.reply(userMention + " 内部エラーが発生しました。\nサーバーが正常に停止できなかった可能性があります。").setEphemeral(false);
						messageAction.queue();
					}
				} else {
					messageAction = e.reply("Terrariaサーバーは現在オフラインです！").setEphemeral(true);
					messageAction.queue();
				}

			}
			case "gcp-status" -> {
				ReplyCallbackAction messageAction;
				if (!require) {
					messageAction = e.reply("コンフィグの設定が不十分なため、コマンドを実行できません。").setEphemeral(true);
					messageAction.queue();
					return;
				}
				
				String gcpChannelId2 = Long.toString(gcpChannelId);
				if (!channelId.equals(gcpChannelId2)) {
					messageAction = e.reply("テラリアのコマンドは " + channelLink + " で実行してください。").setEphemeral(true);
					messageAction.queue();
					return;
				}
				
				if (isgcp()) {
					messageAction = e.reply("Terrariaサーバーは現在オンラインです。").setEphemeral(true);
				} else {
					messageAction = e.reply("Terrariaサーバーは現在オフラインです。").setEphemeral(true);
				}

				messageAction.queue();
			}
			default -> throw new AssertionError();
		}
    }
}
