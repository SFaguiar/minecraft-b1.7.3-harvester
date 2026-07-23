package io.github.sfaguiar.harvester.mixin.client;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the private {@code static Minecraft INSTANCE} field so the
 * client-only config keybind can obtain the running client to open the
 * config screen. A narrow, read-only accessor — it adds no behavior and
 * mutates nothing. Beta 1.7.3 has no public {@code getInstance()} and
 * StationAPI adds none, so this single-method accessor is the minimal way to
 * reach the client instance from a static event listener (there is no
 * {@code Minecraft} reference in scope in {@code KeyStateChangedEvent}).
 *
 * <p>{@code client}-sided in {@code harvester.mixins.json}: never applied on
 * a dedicated server (which never links any client class).
 */
@Mixin(Minecraft.class)
public interface MinecraftInstanceAccessor {

    @Accessor("INSTANCE")
    static Minecraft harvester$getInstance() {
        throw new AssertionError("mixin accessor not applied");
    }
}
