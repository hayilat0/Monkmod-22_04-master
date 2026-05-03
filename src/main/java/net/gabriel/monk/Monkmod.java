package net.gabriel.monk;

import net.fabricmc.api.ModInitializer;
import net.gabriel.monk.effect.ModEffects;
import net.gabriel.monk.entity.ModEntities;
import net.gabriel.monk.event.MonkCombatEvents;
import net.gabriel.monk.event.MonkSphereCapEnforcer;
import net.gabriel.monk.event.MonkVfxEvents;
import net.gabriel.monk.network.EtherealStepVfxPayload;
import net.gabriel.monk.spell.MonkCustomSpellImpact;
import net.gabriel.monk.util.MonkDelayedTasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Monkmod implements ModInitializer {
	public static final String MOD_ID = "monkmod";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerAll();
		ModItemGroups.registerAll();
		ModEffects.registerAll();
		ModEntities.registerAll();

		MonkCustomSpellImpact.registerCustomImpacts();
		MonkCombatEvents.register();
		MonkVfxEvents.register();

		EtherealStepVfxPayload.register();

		// Garante que o limite volta para 5 sem a manopla.
		MonkSphereCapEnforcer.register();

		// Permite executar golpes com pequeno intervalo entre eles.
		MonkDelayedTasks.register();
	}
}