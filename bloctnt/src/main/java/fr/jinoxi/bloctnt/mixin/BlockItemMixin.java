package fr.jinoxi.bloctnt.mixin;

import fr.jinoxi.bloctnt.BlockTimerManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockItemMixin {

	@Inject(method = "place", at = @At("RETURN"))
	private void bloctnt$onPlace(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (!cir.getReturnValue().consumesAction()) return;
		if (!(context.getLevel() instanceof ServerLevel level)) return;

		BlockTimerManager.start(level, context.getClickedPos());
	}
}
