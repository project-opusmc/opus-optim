package org.polydevs.opusmc.optim.renderer;
import java.io.*;import java.net.*;import java.nio.file.*;import java.security.*;import java.util.*;import java.util.regex.*;import java.util.zip.*;
/** Full real M5 class verification in an unsigned resource loader, without game startup. */
public final class HookClassVerify {
    private HookClassVerify() { }
    public static void main(String[] args)throws Exception {
        if(!System.getProperty("java.version").startsWith("1.8."))throw new AssertionError("Java8 required");
        try(GameLoader loader=new GameLoader(Paths.get(args[0]),Paths.get(args[1]))) {
            for(String target:new String[]{"Config","bqs","net.optifine.render.VboRegion"}) {
                Class<?> c=Class.forName(target,false,loader);c.getDeclaredMethods();c.getDeclaredConstructors();
                System.out.println("VERIFIED full Java8 class "+target);
            }
            if(!RendererTransformer.allApplied())throw new AssertionError("Incomplete hook set");
        }
    }
    static final class GameLoader extends ClassLoader implements AutoCloseable {
        final ZipFile mc,of;final URLClassLoader tools;final RendererTransformer transformer=new RendererTransformer(true);
        GameLoader(Path m,Path o)throws Exception{super(HookClassVerify.class.getClassLoader());mc=new ZipFile(m.toFile());of=new ZipFile(o.toFile());tools=new URLClassLoader(new URL[]{o.toUri().toURL()},getParent());}
        @Override protected Class<?> findClass(String name)throws ClassNotFoundException {
            String entry=name.replace('.','/')+".class";
            try {
                byte[] bytes;
                if(of.getEntry(entry)!=null)bytes=read(of.getInputStream(of.getEntry(entry)));
                else if(of.getEntry("patch/"+entry+".xdelta")!=null){Class<?> p=Class.forName("optifine.Patcher",true,tools);Object map=p.getMethod("getConfigurationMap",ZipFile.class).invoke(null,of);Object patterns=p.getMethod("getConfigurationPatterns",Map.class).invoke(null,map);Object provider=Class.forName("optifine.ZipResourceProvider",true,tools).getConstructor(ZipFile.class).newInstance(mc);bytes=(byte[])p.getMethod("applyPatch",String.class,byte[].class,Pattern[].class,Map.class,Class.forName("optifine.IResourceProvider",true,tools)).invoke(null,entry,read(of.getInputStream(of.getEntry("patch/"+entry+".xdelta"))),patterns,map,provider);}
                else if(mc.getEntry(entry)!=null)bytes=read(mc.getInputStream(mc.getEntry(entry)));
                else throw new ClassNotFoundException(name);
                bytes=transformer.transform(name,name,bytes);return defineClass(name,bytes,0,bytes.length);
            } catch(ClassNotFoundException e){throw e;}catch(Exception e){throw new ClassNotFoundException(name,e);}
        }
        public void close()throws IOException{mc.close();of.close();tools.close();}
    }
    private static byte[] read(InputStream in)throws IOException{try(InputStream i=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=i.read(b))!=-1)out.write(b,0,n);return out.toByteArray();}}
}
