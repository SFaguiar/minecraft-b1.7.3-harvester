package io.github.sfaguiar.harvester.client.config;

import io.github.sfaguiar.harvester.client.HarvesterConfigState;
import io.github.sfaguiar.harvester.config.HarvesterConfig;
import io.github.sfaguiar.harvester.config.NeighborhoodChoice;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * The self-contained, autonomous in-game configuration screen — the approved
 * fallback (owner Decision A) after the Glass Config API 3.2.5 spike showed it
 * cannot keep {@code harvester.properties} as the single authoritative source
 * (it owns its own YAML file, its save/load listeners observe rather than
 * replace persistence) and its current line targets Java 21 against this
 * project's pinned Java 17. This screen uses only the vanilla {@link Screen}
 * API plus StationAPI — no external mod dependency, no graphical class on the
 * dedicated server, and it edits the exact same {@code harvester.properties}
 * the loader reads (through {@link HarvesterConfigState#save}).
 *
 * <p>Scope is the "config simples" toggles and {@code maxChain}; the advanced
 * allowlist/denylist and per-tool-ID lists stay file-only for 1.0.0
 * (Decision F). Every value edited here is the client's own local/singleplayer
 * gameplay config — it never influences a dedicated server, which reads its
 * own file and recomputes everything (server authority is untouched). The
 * server-only {@code multiplayerAllowed} key is deliberately not shown.
 */
@Environment(EnvType.CLIENT)
public final class HarvesterConfigScreen extends Screen {

    private static final int[] MAX_CHAIN_STEPS = {8, 16, 32, 64, 100};

    private static final int ID_ENABLED = 1;
    private static final int ID_CONSOLIDATE = 2;
    private static final int ID_LOGS = 3;
    private static final int ID_ORES = 4;
    private static final int ID_DIRT = 5;
    private static final int ID_GRAVEL = 6;
    private static final int ID_LEAVES = 7;
    private static final int ID_CROPS = 8;
    private static final int ID_DIAGNOSTIC = 9;
    private static final int ID_NEIGHBORHOOD = 10;
    private static final int ID_MAX_CHAIN = 11;
    private static final int ID_DONE = 200;

    private final Screen parent;
    private final HarvesterConfig base;

    private boolean enabled;
    private boolean consolidateDrops;
    private boolean harvestLogs;
    private boolean harvestOres;
    private boolean harvestDirt;
    private boolean harvestGravel;
    private boolean harvestLeaves;
    private boolean harvestCrops;
    private boolean diagnosticLogging;
    private NeighborhoodChoice neighborhood;
    private int maxChain;

    public HarvesterConfigScreen(Screen parent) {
        this.parent = parent;
        this.base = HarvesterConfigState.current();
        this.enabled = base.enabled();
        this.consolidateDrops = base.consolidateDrops();
        this.harvestLogs = base.harvestLogs();
        this.harvestOres = base.harvestOres();
        this.harvestDirt = base.harvestDirt();
        this.harvestGravel = base.harvestGravel();
        this.harvestLeaves = base.harvestLeaves();
        this.harvestCrops = base.harvestCrops();
        this.diagnosticLogging = base.diagnosticLogging();
        this.neighborhood = base.neighborhood();
        this.maxChain = base.maxChain();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        this.buttons.clear();
        int colLeft = this.width / 2 - 155;
        int colRight = this.width / 2 + 5;
        int w = 150;
        int h = 20;
        int top = 40;
        int row = 24;

        this.buttons.add(new ButtonWidget(ID_ENABLED, colLeft, top, w, h, labelEnabled()));
        this.buttons.add(new ButtonWidget(ID_CONSOLIDATE, colRight, top, w, h, labelConsolidate()));
        this.buttons.add(new ButtonWidget(ID_LOGS, colLeft, top + row, w, h, labelLogs()));
        this.buttons.add(new ButtonWidget(ID_ORES, colRight, top + row, w, h, labelOres()));
        this.buttons.add(new ButtonWidget(ID_DIRT, colLeft, top + row * 2, w, h, labelDirt()));
        this.buttons.add(new ButtonWidget(ID_GRAVEL, colRight, top + row * 2, w, h, labelGravel()));
        this.buttons.add(new ButtonWidget(ID_LEAVES, colLeft, top + row * 3, w, h, labelLeaves()));
        this.buttons.add(new ButtonWidget(ID_CROPS, colRight, top + row * 3, w, h, labelCrops()));
        this.buttons.add(new ButtonWidget(ID_NEIGHBORHOOD, colLeft, top + row * 4, w, h, labelNeighborhood()));
        this.buttons.add(new ButtonWidget(ID_MAX_CHAIN, colRight, top + row * 4, w, h, labelMaxChain()));
        this.buttons.add(new ButtonWidget(ID_DIAGNOSTIC, colLeft, top + row * 5, w, h, labelDiagnostic()));
        this.buttons.add(new ButtonWidget(ID_DONE, this.width / 2 - 100, top + row * 6 + 8, 200, h, "Done"));
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        switch (button.id) {
            case ID_ENABLED: enabled = !enabled; button.text = labelEnabled(); break;
            case ID_CONSOLIDATE: consolidateDrops = !consolidateDrops; button.text = labelConsolidate(); break;
            case ID_LOGS: harvestLogs = !harvestLogs; button.text = labelLogs(); break;
            case ID_ORES: harvestOres = !harvestOres; button.text = labelOres(); break;
            case ID_DIRT: harvestDirt = !harvestDirt; button.text = labelDirt(); break;
            case ID_GRAVEL: harvestGravel = !harvestGravel; button.text = labelGravel(); break;
            case ID_LEAVES: harvestLeaves = !harvestLeaves; button.text = labelLeaves(); break;
            case ID_CROPS: harvestCrops = !harvestCrops; button.text = labelCrops(); break;
            case ID_DIAGNOSTIC: diagnosticLogging = !diagnosticLogging; button.text = labelDiagnostic(); break;
            case ID_NEIGHBORHOOD: cycleNeighborhood(); button.text = labelNeighborhood(); break;
            case ID_MAX_CHAIN: cycleMaxChain(); button.text = labelMaxChain(); break;
            case ID_DONE:
                this.minecraft.setScreen(parent);
                return;
            default:
                return;
        }
        apply();
    }

    private void cycleNeighborhood() {
        neighborhood = neighborhood == NeighborhoodChoice.LEGACY_26
                ? NeighborhoodChoice.ORTHOGONAL_6 : NeighborhoodChoice.LEGACY_26;
    }

    private void cycleMaxChain() {
        int next = MAX_CHAIN_STEPS[0];
        for (int i = 0; i < MAX_CHAIN_STEPS.length; i++) {
            if (MAX_CHAIN_STEPS[i] == maxChain) {
                next = MAX_CHAIN_STEPS[(i + 1) % MAX_CHAIN_STEPS.length];
                break;
            }
            if (MAX_CHAIN_STEPS[i] > maxChain) {
                next = MAX_CHAIN_STEPS[i];
                break;
            }
        }
        maxChain = next;
    }

    /** Rebuilds from {@link #base} (preserving file-only fields) and persists through the loader. */
    private void apply() {
        HarvesterConfig updated = base.toBuilder()
                .enabled(enabled)
                .consolidateDrops(consolidateDrops)
                .harvestLogs(harvestLogs)
                .harvestOres(harvestOres)
                .harvestDirt(harvestDirt)
                .harvestGravel(harvestGravel)
                .harvestLeaves(harvestLeaves)
                .harvestCrops(harvestCrops)
                .diagnosticLogging(diagnosticLogging)
                .neighborhood(neighborhood)
                .maxChain(maxChain)
                .build();
        HarvesterConfigState.save(updated);
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        renderBackground();
        drawCenteredTextWithShadow(this.textRenderer, "Harvester", this.width / 2, 15, 0xFFFFFF);
        drawCenteredTextWithShadow(
                this.textRenderer, "Client / singleplayer settings — servers use their own config",
                this.width / 2, 27, 0xA0A0A0
        );
        super.render(mouseX, mouseY, delta);
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private String labelEnabled() { return "Harvester: " + onOff(enabled); }
    private String labelConsolidate() { return "Consolidate drops: " + onOff(consolidateDrops); }
    private String labelLogs() { return "Logs (axe): " + onOff(harvestLogs); }
    private String labelOres() { return "Ores (pickaxe): " + onOff(harvestOres); }
    private String labelDirt() { return "Dirt (shovel): " + onOff(harvestDirt); }
    private String labelGravel() { return "Gravel (shovel): " + onOff(harvestGravel); }
    private String labelLeaves() { return "Leaves (shears): " + onOff(harvestLeaves); }
    private String labelCrops() { return "Crops (hoe): " + onOff(harvestCrops); }
    private String labelDiagnostic() { return "Diagnostic log: " + onOff(diagnosticLogging); }
    private String labelNeighborhood() { return "Neighborhood: " + neighborhood.propertyValue(); }
    private String labelMaxChain() { return "Max chain: " + maxChain; }
}
