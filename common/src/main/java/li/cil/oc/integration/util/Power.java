package li.cil.oc.integration.util;

import li.cil.oc.Settings;

public final class Power {
    private Power() {
    }

    // Applied Energistics 2

    public static double fromAE(double value) {
        return value * Settings.get().ratioAppliedEnergistics2;
    }

    public static double toAE(double value) {
        return value / Settings.get().ratioAppliedEnergistics2;
    }

    // Factorization

    public static double fromCharge(double value) {
        return value * Settings.get().ratioFactorization;
    }

    public static double toCharge(double value) {
        return value / Settings.get().ratioFactorization;
    }

    // Galacticraft

    public static double fromGC(float value) {
        return value * Settings.get().ratioGalacticraft;
    }

    public static float toGC(double value) {
        return (float) (value / Settings.get().ratioGalacticraft);
    }

    // IndustrialCraft 2

    public static double fromEU(double value) {
        return value * Settings.get().ratioIndustrialCraft2;
    }

    public static double toEU(double value) {
        return value / Settings.get().ratioIndustrialCraft2;
    }

    // Mekanism

    public static double fromJoules(double value) {
        return value * Settings.get().ratioMekanism;
    }

    public static double toJoules(double value) {
        return value / Settings.get().ratioMekanism;
    }

    // Redstone Flux

    public static double fromRF(int value) {
        return value * Settings.get().ratioRedstoneFlux;
    }

    public static int toRF(double value) {
        return (int) (value / Settings.get().ratioRedstoneFlux);
    }

    // RotaryCraft

    public static double fromWA(long value) {
        return value * Settings.get().ratioRotaryCraft;
    }

    public static long toWA(double value) {
        return (long) (value / Settings.get().ratioRotaryCraft);
    }

    // Tesla

    public static double fromTesla(long value) {
        return value * Settings.get().ratioRedstoneFlux;
    }

    public static long toTesla(double value) {
        return (long) (value / Settings.get().ratioRedstoneFlux);
    }

    // Forge Energy / Team Reborn Energy (li.cil.oc.common.transfer.EnergyHandler units).
    // Like on 1.16.5 (which used fromRF/toRF for Forge Energy) this uses the Redstone Flux ratio.

    public static double fromFE(long value) {
        return value * Settings.get().ratioRedstoneFlux;
    }

    public static long toFE(double value) {
        return (long) (value / Settings.get().ratioRedstoneFlux);
    }
}
