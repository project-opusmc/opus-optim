package org.polydevs.opusmc.optim.renderer;
import java.io.File;import java.util.List;import net.minecraft.launchwrapper.ITweaker;import net.minecraft.launchwrapper.LaunchClassLoader;
/** Secondary standalone tweak; OptiFine/Opus retain launch arguments and target. */
public final class OptimRendererTweaker implements ITweaker {
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) { }
    public void injectIntoClassLoader(LaunchClassLoader loader) {
        for(String prefix:new String[]{"org.polydevs.opusmc.optim.renderer.","org.embeddedt.embeddium.","org.taumc.celeritas.","common.xyz.wagyourtail.","it.unimi.dsi.fastutil.","org.joml."})loader.addClassLoaderExclusion(prefix);
        loader.registerTransformer("org.polydevs.opusmc.optim.renderer.RendererTransformer");
        System.out.println("[O2] renderer transformer REGISTERED; selection="+System.getProperty("opus.optim.renderer","off"));
    }
    public String getLaunchTarget() { return "net.minecraft.client.main.Main"; }
    public String[] getLaunchArguments() { return new String[0]; }
}
