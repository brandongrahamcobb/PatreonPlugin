/*  Predicator.java The purpose of this program is to run lambda functions before executing code on the main thread.
 *
 *  Copyright (C) 2025  github.com/brandongrahamcobb
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.brandongcobb.vyrtuous.utils.handlers;

import com.brandongcobb.vyrtuous.Vyrtuous;
import com.brandongcobb.vyrtuous.bots.DiscordBot;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;

public class Predicator {

    private Vyrtuous app;
    private JDA bot;
    private ConfigManager cm;

    public Predicator(ConfigManager cm, JDA bot) {
        this.cm = cm.completeGetInstance();
        this.bot = bot;
    }

    public CompletableFuture<Boolean> atHome(Guild guild) {
        if (guild == null) return CompletableFuture.completedFuture(false);
        return cm.completeGetConfigValue("discord_testing_guild_id", Long.class)
            .thenApply(id -> Long.parseLong(guild.getId()) == (long) id);
    }

    public CompletableFuture<Boolean> releaseMode(User user, TextChannel channel) {
        CompletableFuture<Long> ownerIdFuture = cm.completeGetConfigValue("discord_owner_id", Long.class);
        CompletableFuture<Boolean> releaseModeFuture = cm.completeGetConfigValue("discord_release_mode", Boolean.class);
        return ownerIdFuture.thenCombine(releaseModeFuture, (ownerId, releaseMode) ->
            user.getIdLong() == ownerId ||
            (boolean) releaseMode ||
            channel instanceof PrivateChannel
        );
    }

    public CompletableFuture<Boolean> isDeveloper(User user) {
        if (user == null) return CompletableFuture.completedFuture(false);
        return cm.completeGetConfigValue("discord_owner_id", Long.class)
            .thenApply(ownerId -> user.getIdLong() == (Long) ownerId);
    }

    public CompletableFuture<Boolean> isVeganUser(User user) {
        return cm.completeGetConfigValue("discord_testing_guild_ids", List.class)
            .thenCompose(obj -> {
                List<Long> guildIds = (List<Long>) obj;
                CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
                for (Long guildId : guildIds) {
                    result = result.thenCompose(res -> getGuildById(guildId).thenCompose(guild -> {
                        if (guild != null) {
                            Member member = guild.getMember(user);
                            if (member == null) return CompletableFuture.completedFuture(false);
                            Optional<Role> veganRoleOpt = guild.getRoles().stream()
                                .filter(role -> role.getName().equalsIgnoreCase("vegan"))
                                .findFirst();
                            if (veganRoleOpt.isPresent()) {
                                Role veganRole = veganRoleOpt.get();
                                if (guild.getMembersWithRoles(veganRole).contains(member)) {
                                    return CompletableFuture.completedFuture(true);
                                }
                            }
                        }
                        return CompletableFuture.completedFuture(false);
                    }));
                }
                return result;
            });
    }

    public CompletableFuture<Guild> getGuildById(long guildId) {
        return CompletableFuture.supplyAsync(() -> {
            for (Guild guild : bot.getGuilds()) {
                if (Long.parseLong(guild.getId()) == guildId) {
                    return guild;
                }
            }
            return null;
        });
    }

    public CompletableFuture<Boolean> isReleaseMode(TextChannel channel, User user) {
        CompletableFuture<Long> ownerIdFuture = cm.completeGetConfigValue("discord_owner_id", Long.class);
        CompletableFuture<Boolean> releaseModeFuture = cm.completeGetConfigValue("discord_release_mode", Boolean.class);
        return ownerIdFuture.thenCombine(releaseModeFuture, (ownerId, releaseMode) ->
            user.getIdLong() == ownerId ||
            (boolean) releaseMode ||
            channel instanceof PrivateChannel
        );
    }
}
