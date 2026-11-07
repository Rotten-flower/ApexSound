package dev.flower.apex_sound_effects.mixin;

import dev.flower.apex_sound_effects.HitFeedback;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayDeque;
import java.util.Deque;

/** 在伤害调用结束后读取结果，覆盖致命和取消伤害。 / Observes the completed hurt call, including fatal and cancelled damage. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Unique private final Deque<Float> apexSoundEffects$healthBeforeDamage = new ArrayDeque<>();
    @Inject(method="hurt",at=@At("HEAD"))
    private void apexSoundEffects$before(DamageSource source,float amount,CallbackInfoReturnable<Boolean> callback) {
        LivingEntity target=(LivingEntity)(Object)this;
        if (!target.level().isClientSide) apexSoundEffects$healthBeforeDamage.push(target.getHealth());
    }
    @Inject(method="hurt",at=@At("RETURN"))
    private void apexSoundEffects$after(DamageSource source,float amount,CallbackInfoReturnable<Boolean> callback) {
        if (apexSoundEffects$healthBeforeDamage.isEmpty()) return;
        HitFeedback.captureAfter((LivingEntity)(Object)this,source,callback.getReturnValue(),apexSoundEffects$healthBeforeDamage.pop());
    }
}
