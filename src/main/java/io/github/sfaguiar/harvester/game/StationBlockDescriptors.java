package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.core.BlockDescriptor;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.registry.tag.BlockTags;
import net.modificationstation.stationapi.api.tag.TagKey;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts a real StationAPI {@link BlockState} into a pure
 * {@link BlockDescriptor} — the only place in this codebase that decides
 * <em>how</em> a block is classified. {@code core} never sees a
 * {@link BlockState}, {@link TagKey}, or {@link Identifier}. Side-agnostic
 * (takes a {@code BlockState}, never a {@code World} subtype), so both the
 * singleplayer client and the multiplayer server call this exact method —
 * see {@code ARCHITECTURE.md}, "Reuse vs new adapter".
 *
 * <p>Specific ore tags are detected generically — {@code namespace=c},
 * {@code path} starting with {@code "ores/"} with a non-empty suffix — by
 * enumerating every tag the block actually carries via
 * {@link BlockState#streamTags()}, never by checking a fixed list of
 * {@link BlockTags} constants. This is what lets a correctly-tagged modded
 * ore (e.g. {@code c:ores/tin}) participate without any code change here:
 * confirmed against the pinned StationAPI {@code station-vanilla-fix-v0}
 * tag data, where {@code c:ores} is itself composed of exactly the six
 * specific {@code c:ores/<material>} tags (never a block ID list), and
 * {@code c:ores/redstone} already lists both
 * {@code minecraft:redstone_ore} and {@code minecraft:redstone_ore_lit}.
 */
public final class StationBlockDescriptors {

    private static final String SPECIFIC_ORE_NAMESPACE = "c";
    private static final String SPECIFIC_ORE_PATH_PREFIX = "ores/";

    private StationBlockDescriptors() {
    }

    public static BlockDescriptor describe(BlockState state) {
        boolean log = state.isIn(BlockTags.LOGS);
        Set<String> specificOreTags = state.streamTags()
                .map(TagKey::id)
                .filter(StationBlockDescriptors::isSpecificOreTag)
                .map(Identifier::toString)
                .collect(Collectors.toUnmodifiableSet());
        boolean genericOre = state.isIn(BlockTags.ORES);
        Identifier registryId = BlockRegistry.INSTANCE.getId(state.getBlock());
        String registryIdentity = registryId != null ? registryId.toString() : "";
        return new BlockDescriptor(log, specificOreTags, genericOre, registryIdentity);
    }

    private static boolean isSpecificOreTag(Identifier tagId) {
        return SPECIFIC_ORE_NAMESPACE.equals(tagId.namespace.toString())
                && tagId.path.startsWith(SPECIFIC_ORE_PATH_PREFIX)
                && tagId.path.length() > SPECIFIC_ORE_PATH_PREFIX.length();
    }
}
