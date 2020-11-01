package de.staticred.discordbot.util;

import de.staticred.discordbot.DBVerifier;
import de.staticred.discordbot.api.EventManager;
import de.staticred.discordbot.db.VerifyDAO;
import de.staticred.discordbot.api.event.UserUpdatedRolesEvent;
import de.staticred.discordbot.files.ConfigFileManager;
import de.staticred.discordbot.files.DiscordFileManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MemberManager {

    public static Member getMemberFromPlayer(UUID uuid) throws SQLException {
        User u;

        String id = VerifyDAO.INSTANCE.getDiscordID(uuid);

        u = DBVerifier.getInstance().jda.retrieveUserById(id).complete();
        Member m = null;
        if(DBVerifier.getInstance().debugMode) Debugger.debugMessage(id);

        if (!DBVerifier.getInstance().jda.getGuilds().isEmpty()) {
            for (Guild guild : DBVerifier.getInstance().jda.getGuilds()) {
                if (u != null)
                    m = guild.retrieveMember(u).complete();
            }
        } else {
            throw new SQLException("There was an internal error! The member of the player can´t be found. Please contact the developer of this plugin.") ;
        }
        return m;
    }

    public static void updateRoles(Member m, ProxiedPlayer p) {
        List<String> roles = new ArrayList<>();
        
        Debugger.debugMessage("Updating groups for player: " + p.getName() + " Member: " + m.getEffectiveName());

        Debugger.debugMessage("Starting group loop");
        for(String group : DiscordFileManager.INSTANCE.getAllGroups()) {
            Debugger.debugMessage("Checking if group " + group + " is a dynamic group.");
            if(!DiscordFileManager.INSTANCE.isDynamicGroup(group)) {
                Debugger.debugMessage("Group is not dynamic.");
                Debugger.debugMessage("Checking if players has permission: " + DiscordFileManager.INSTANCE.getPermissionsForGroup(group));
                if(p.hasPermission(DiscordFileManager.INSTANCE.getPermissionsForGroup(group))) {
                    Debugger.debugMessage("Player has permission.");
                    roles.add(group);
                } else {
                    Debugger.debugMessage("Player does not have permission.");
                }
            }
        }

        List<Role> userRoles = roles.stream().map(role -> {
            String groupName = DiscordFileManager.INSTANCE.getDiscordGroupNameForGroup(role);
            List<Role> rolesByName = m.getGuild().getRolesByName(groupName, true);
            return rolesByName.get(0);
        }).collect(Collectors.toList());

        if(!roles.isEmpty()) {
            String role = roles.get(roles.size() - 1);
            String newNick = DiscordFileManager.INSTANCE.getPrefix(role).replaceAll("%name%", p.getName());
            String nick = m.getNickname();
            if(nick != null && !nick.equals(newNick)) {
                m.getGuild().modifyNickname(m, newNick).complete();
            }
        }

        if(!m.getRoles().equals(userRoles)) {
            m.getGuild().modifyMemberRoles(m, userRoles).complete();
        }

        EventManager.instance.fireEvent(new UserUpdatedRolesEvent(m,p,roles));
    }

}
