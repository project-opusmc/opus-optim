package org.polydevs.opusmc.optim.renderer;
import org.junit.jupiter.api.Test;import static org.junit.jupiter.api.Assertions.*;
final class RegionLifetimeTest {
    @Test void worldResetReclaimsRegionsNotReleasedByTheHostAndIsIdempotent(){RegionLifetime scope=new RegionLifetime();RegionRendererTest.Storage[] stores=new RegionRendererTest.Storage[32];RegionRenderer[] regions=new RegionRenderer[32];for(int i=0;i<32;i++){stores[i]=new RegionRendererTest.Storage();regions[i]=new RegionRenderer(28,stores[i]);scope.add(regions[i]);}for(int i=0;i<16;i++)assertTrue(scope.release(regions[i]));assertEquals(16,scope.size());assertEquals(16,scope.reset());assertEquals(0,scope.size());for(RegionRendererTest.Storage s:stores)assertTrue(s.closed);assertEquals(0,scope.reset());assertFalse(scope.release(regions[0]));}
}
