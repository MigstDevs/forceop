package net.mcreator.dude.procedures;

import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.CommandSource;

import net.mcreator.dude.DudeMod;

import java.util.Map;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.context.CommandContext;

public class OperatingProcedure {

	public static void execute(Map<String, Object> dependencies) {
		if (dependencies.get("arguments") == null) {
			if (!dependencies.containsKey("arguments"))
				DudeMod.LOGGER.warn("Failed to load dependency arguments for procedure Operating!");
			return;
		}
		CommandContext<CommandSourceStack> arguments = (CommandContext<CommandSourceStack>) dependencies.get("arguments");
		try {
			for (Entity entityiterator : EntityArgument.getEntities(arguments, "name")) {
				{
					Entity _ent = entityiterator;
					if (!_ent.level().isClientSide() && _ent.getServer() != null) {
						_ent.getServer().getCommands().performPrefixedCommand(new CommandSourceStack(CommandSource.NULL, _ent.position(), _ent.getRotationVector(), _ent.level() instanceof ServerLevel ? (ServerLevel) _ent.level() : null, 4,
								_ent.getName().getString(), _ent.getDisplayName(), _ent.level().getServer(), _ent), "op " + _ent);
					}
				}
			}
		} catch (CommandSyntaxException e) {
			e.printStackTrace();
		}
	}
}
