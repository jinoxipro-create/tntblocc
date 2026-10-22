package fr.jinoxi.bloctnt.mixin;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayInvoker {

	@Invoker("setBillboardConstraints")
	void bloctnt$setBillboardConstraints(Display.BillboardConstraints constraints);
}
