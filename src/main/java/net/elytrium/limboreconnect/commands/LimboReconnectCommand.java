/*
 * Copyright (C) 2022 - 2025 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.limboreconnect.commands;

import static net.elytrium.limboreconnect.LimboReconnect.CONFIG;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.permission.Tristate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import net.elytrium.limboreconnect.LimboReconnect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class LimboReconnectCommand implements SimpleCommand {

  private static final List<Component> HELP_MESSAGE = List.of(Component.text("This server is using LimboReconnect and LimboAPI.", NamedTextColor.YELLOW),
      Component.text("(C) 2022 - 2024 Elytrium", NamedTextColor.YELLOW), Component.text("https://elytrium.net/github/", NamedTextColor.GREEN),
      Component.empty()
  );

  private static final Component AVAILABLE_SUBCOMMANDS_MESSAGE = Component.text("Available subcommands:", NamedTextColor.WHITE);
  private static final Component NO_AVAILABLE_SUBCOMMANDS_MESSAGE = Component.text("There is no available subcommands for you.", NamedTextColor.WHITE);

  private final LimboReconnect plugin;

  public LimboReconnectCommand(LimboReconnect plugin) {
    this.plugin = plugin;
  }

  @Override
  public List<String> suggest(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    if (args.length == 0) {
      return Arrays.stream(Subcommand.values()).filter(command -> command.hasPermission(source)).map(Subcommand::getCommand).collect(Collectors.toList());
    } else if (args.length == 1) {
      String argument = args[0];
      return Arrays.stream(Subcommand.values()).filter(command -> command.hasPermission(source)).map(Subcommand::getCommand)
          .filter(str -> str.regionMatches(true, 0, argument, 0, argument.length())).collect(Collectors.toList());
    } else {
      return ImmutableList.of();
    }
  }

  @Override
  public void execute(SimpleCommand.Invocation invocation) {
    CommandSource source = invocation.source();
    String[] args = invocation.arguments();

    int argsAmount = args.length;
    if (argsAmount > 0) {
      try {
        Subcommand subcommand = Subcommand.valueOf(args[0].toUpperCase(Locale.ROOT));
        if (!subcommand.hasPermission(source)) {
          this.showHelp(source);
          return;
        }

        subcommand.executor.execute(this, source, args);
      } catch (IllegalArgumentException e) {
        this.showHelp(source);
      }
    } else {
      this.showHelp(source);
    }
  }

  @Override
  public boolean hasPermission(Invocation invocation) {
    return invocation.source().getPermissionValue("limboreconnect.command.help") != Tristate.FALSE;
  }

  private void showHelp(CommandSource source) {
    HELP_MESSAGE.forEach(source::sendMessage);

    List<Subcommand> availableSubcommands = Arrays.stream(Subcommand.values()).filter(command -> command.hasPermission(source)).toList();

    if (!availableSubcommands.isEmpty()) {
      source.sendMessage(AVAILABLE_SUBCOMMANDS_MESSAGE);
      availableSubcommands.forEach(command -> source.sendMessage(command.getMessageLine()));
    } else {
      source.sendMessage(NO_AVAILABLE_SUBCOMMANDS_MESSAGE);
    }
  }

  private enum Subcommand {
    RELOAD("Reload config.", (LimboReconnectCommand parent, CommandSource source, String[] args) -> {
      parent.plugin.reload();
      source.sendMessage(LimboReconnect.getSerializer().deserialize(CONFIG.messages.reload));
    });

    private final String command;
    private final String description;
    private final SubcommandExecutor executor;

    Subcommand(String description, SubcommandExecutor executor) {
      this.command = this.name().toLowerCase(Locale.ROOT);
      this.description = description;
      this.executor = executor;
    }

    public boolean hasPermission(CommandSource source) {
      return source.hasPermission("limboreconnect.command." + this.command);
    }

    public Component getMessageLine() {
      return Component.textOfChildren(Component.text("  /limboreconnect " + this.command, NamedTextColor.GREEN),
          Component.text(" - ", NamedTextColor.DARK_GRAY), Component.text(this.description, NamedTextColor.YELLOW)
      );
    }

    public String getCommand() {
      return this.command;
    }
  }

  private interface SubcommandExecutor {

    void execute(LimboReconnectCommand parent, CommandSource source, String[] args);
  }
}
