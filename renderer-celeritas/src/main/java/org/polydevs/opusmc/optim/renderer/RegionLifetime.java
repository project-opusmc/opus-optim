package org.polydevs.opusmc.optim.renderer;
import java.util.*;
/** Explicit world ownership also reclaims regions lost by a host render-list reload. */
final class RegionLifetime {
    private final Set<RegionRenderer> live=Collections.newSetFromMap(new IdentityHashMap<RegionRenderer,Boolean>());
    void add(RegionRenderer region){live.add(region);}
    boolean release(RegionRenderer region){region.close();return live.remove(region);}
    int reset(){int count=live.size();for(RegionRenderer region:live)region.close();live.clear();return count;}
    int size(){return live.size();}
}
