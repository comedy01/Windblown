package dev.windblown.client.gui;

import dev.windblown.client.WindblownClient;
import dev.windblown.client.Texts;
import dev.windblown.config.WindblownConfig;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class WindblownSettingsScreen extends WindblownOptionsScreen {
    private static final int WIDTH = 150;
    private static final int HEIGHT = 20;

    public WindblownSettingsScreen(Screen lastScreen, Options options) {
        super(lastScreen, options, Texts.translatable("windblown.options.title"));
    }

    @Override
    protected void addOptions() {
        WindblownConfig config = WindblownClient.config();

        addRow(
                toggle("windblown.options.enabled", config::enabled, config::setEnabled),
                tooltip(new PercentSlider("windblown.options.strength", WindblownConfig.MIN_STRENGTH_PERCENT,
                        WindblownConfig.MAX_STRENGTH_PERCENT, config.strength(), config::setStrength),
                        Texts.translatable("windblown.options.strength.tooltip")));
        addRow(
                toggle("windblown.options.grass", config::grass, config::setGrass),
                toggle("windblown.options.crops", config::crops, config::setCrops));
        addRow(
                toggle("windblown.options.leaves", config::leaves, config::setLeaves),
                toggle("windblown.options.vines", config::vines, config::setVines));
        addRow(
                toggle("windblown.options.water_plants", config::waterPlants, config::setWaterPlants),
                toggle("windblown.options.weather", config::weather, config::setWeather));
        addRow(
                toggle("windblown.options.rustle", config::rustle, config::setRustle),
                tooltip(new PercentSlider("windblown.options.rustle_volume", 0, WindblownConfig.MAX_VOLUME_PERCENT,
                        config.rustleVolume(), config::setRustleVolume),
                        Texts.translatable("windblown.options.rustle_volume.tooltip")));
        addRow(resetButton(config), null);
    }

    @Override
    public void removed() {
        super.removed();
        WindblownClient.saveConfig();
    }

    private AbstractWidget resetButton(WindblownConfig config) {
        return tooltip(button(Texts.translatable("windblown.options.reset"), WIDTH, button -> {
            config.resetToDefaults();
            ScreenOpener.open(minecraft, new WindblownSettingsScreen(lastScreen, options));
        }), Texts.translatable("windblown.options.reset.tooltip"));
    }

    private AbstractWidget toggle(String key, BooleanSupplier getter, Consumer<Boolean> setter) {
        return tooltip(button(toggleLabel(key, getter.getAsBoolean()), WIDTH, button -> {
            setter.accept(!getter.getAsBoolean());
            button.setMessage(toggleLabel(key, getter.getAsBoolean()));
        }), Texts.translatable(key + ".tooltip"));
    }

    private static Component percentText(int value) {
        return Texts.literal(value + "%");
    }

    private static Component valueLabel(String key, Component value) {
        return Texts.translatable("options.generic_value", Texts.translatable(key), value);
    }

    private static Component toggleLabel(String key, boolean on) {
        return valueLabel(key, Texts.translatable(on ? "options.on" : "options.off"));
    }

    private static final class PercentSlider extends AbstractSliderButton {
        private static final int STEP = 5;

        private final String key;
        private final int min;
        private final int max;
        private final Consumer<Float> onChange;

        PercentSlider(String key, int min, int max, float initial, Consumer<Float> onChange) {
            super(0, 0, WIDTH, HEIGHT, Texts.empty(), (Math.round(initial * 100) - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.onChange = onChange;
            updateMessage();
        }

        private int percent() {
            int raw = min + (int) Math.round(value * (max - min));
            return Math.max(min, Math.min(max, Math.round(raw / (float) STEP) * STEP));
        }

        @Override
        protected void updateMessage() {
            setMessage(valueLabel(key, percentText(percent())));
        }

        @Override
        protected void applyValue() {
            int percent = percent();
            value = (percent - min) / (double) (max - min);
            onChange.accept(percent / 100f);
        }
    }
}
