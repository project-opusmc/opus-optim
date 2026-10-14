package org.polydevs.opusmc.optim.smoke;
import java.net.*;import java.nio.file.*;import java.util.*;import org.polydevs.opusmc.optim.platform.mc189.*;import org.polydevs.opusmc.optim.core.render.GlyphIndex;
public class FullFontInitProbe {
 public static void main(String[] args)throws Exception {
 Path mc=Paths.get("/Users/zvwgvx/.opus-launcher/minecraft/versions/1.8.9/1.8.9.jar"),of=Paths.get("/Users/zvwgvx/.opus-launcher/game/mods/OptiFine_1.8.9_HD_U_M5.jar");
 byte[] original=FrustumVerify.extract(mc,of,"avn"),patched=new PipelinePatch(PipelineOptimizations.Kind.GLYPH_LOOKUP).transform("avn",original);
 List<URL> urls=new ArrayList<>();urls.add(of.toUri().toURL());urls.add(mc.toUri().toURL());
 Path libraries=Paths.get("/Users/zvwgvx/.opus-launcher/minecraft/libraries");try(java.util.stream.Stream<Path> files=Files.walk(libraries)){files.filter(p->p.toString().endsWith(".jar")).forEach(p->{try{urls.add(p.toUri().toURL());}catch(Exception e){throw new RuntimeException(e);}});}
 for(int arm=0;arm<2;arm++){final byte[] bytes=arm==0?original:patched;
 try(URLClassLoader loader=new URLClassLoader(urls.toArray(new URL[0]),FullFontInitProbe.class.getClassLoader()){
 @Override protected Class<?> findClass(String name)throws ClassNotFoundException {if(name.equals("avn")){return defineClass(name,bytes,0,bytes.length);}if(name.indexOf('.')<0){try{byte[] source=FrustumVerify.extract(mc,of,name);return defineClass(name,source,0,source.length);}catch(Exception failure){throw new ClassNotFoundException(name,failure);}}return super.findClass(name);}}){
 Class<?> font=Class.forName("avn",true,loader);java.lang.reflect.Field pages=font.getDeclaredField("c");pages.setAccessible(true);if(java.lang.reflect.Array.getLength(pages.get(null))!=256)throw new AssertionError("Original clinit lost");
 if(arm==1){java.lang.reflect.Field table=font.getDeclaredField("o2$glyphIndex");table.setAccessible(true);short[] entries=(short[])table.get(null);if(entries==null||GlyphIndex.lookup(entries,'A')<0)throw new AssertionError("Table not initialized");System.out.println("FULL_FONT_DEFINE_CLINIT_PASS tableBytes="+entries.length*2+" originalPages=256");}
 else System.out.println("BASE_FONT_DEFINE_CLINIT_PASS originalPages=256");
 }} }
}