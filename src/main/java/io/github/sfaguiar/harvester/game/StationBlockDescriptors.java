package io.github.sfaguiar.harvester.game;

import io.github.sfaguiar.harvester.core.BlockDescriptor;
import net.minecraft.block.Block;
import net.modificationstation.stationapi.api.block.BlockState;
import net.modificationstation.stationapi.api.registry.BlockRegistry;
import net.modificationstation.stationapi.api.registry.tag.BlockTags;
import net.modificationstation.stationapi.api.tag.TagKey;
import net.modificationstation.stationapi.api.util.Identifier;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Converts a real StationAPI {@link BlockState} plus its raw metadata into a
 * pure {@link BlockDescriptor} — the only place in this codebase that decides
 * <em>how</em> a block is classified. {@code core} never sees a
 * {@link BlockState}, {@link TagKey}, {@link Identifier}, or {@link Block}.
 * Side-agnostic (takes a {@code BlockState}, never a {@code World} subtype),
 * so both the singleplayer client and the multiplayer server call this exact
 * method.
 *
 * <p>Logs and ores are tag-driven exactly as before. The 1.0.0 categories
 * that StationAPI has no tag for on the pinned platform — dirt, gravel,
 * leaves, and crops — are identified by comparing against the vanilla
 * {@link Block} constants ({@code Block.DIRT}/{@code GRAVEL}/{@code LEAVES}/
 * {@code WHEAT}), and their metadata-sensitive behavior (leaf species,
 * crop maturity) is carried through {@link BlockDescriptor#metadata()} — the
 * raw metadata the caller read from the world alongside the state, since a
 * tag cannot distinguish it (StationAPI issue {@code #234}).
 */
public final class StationBlockDescriptors {

    private static final String SPECIFIC_ORE_NAMESPACE = "c";
    private static final String SPECIFIC_ORE_PATH_PREFIX = "ores/";

    private StationBlockDescriptors() {
    }

    public static BlockDescriptor describe(BlockState state, int metadata) {
        boolean log = state.isIn(BlockTags.LOGS);
        Set<String> specificOreTags = state.streamTags()
                .map(TagKey::id)
                .filter(StationBlockDescriptors::isSpecificOreTag)
                .map(Identifier::toString)
                .collect(Collectors.toUnmodifiableSet());
        boolean genericOre = state.isIn(BlockTags.ORES);
        Block block = state.getBlock();
        Identifier registryId = BlockRegistry.INSTANCE.getId(block);
        String registryIdentity = registryId != null ? registryId.toString() : "";
        boolean dirt = block == Block.DIRT;
        boolean gravel = block == Block.GRAVEL;
        boolean leaves = block == Block.LEAVES;
        boolean crop = block == Block.WHEAT;
        return new BlockDescriptor(
                log, specificOreTags, genericOre, registryIdentity, metadata, dirt, gravel, leaves, crop
        );
    }

    private static boolean isSpecificOreTag(Identifier tagId) {
        return SPECIFIC_ORE_NAMESPACE.equals(tagId.namespace.toString())
                && tagId.path.startsWith(SPECIFIC_ORE_PATH_PREFIX)
                && tagId.path.length() > SPECIFIC_ORE_PATH_PREFIX.length();
    }
}
