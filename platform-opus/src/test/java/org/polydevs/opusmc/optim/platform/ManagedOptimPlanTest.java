package org.polydevs.opusmc.optim.platform;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.optim.core.render.GlyphIndex;
import static org.junit.jupiter.api.Assertions.*;

final class ManagedOptimPlanTest {
    @Test void absentSelectionHasNoRegistrationsAndKnownExplicitPatchesRetainExactTargets() {
        assertTrue(ManagedOptimPlan.prepare("", "", getClass().getClassLoader()).registrations().isEmpty());
        ProviderPlan plan=ManagedOptimPlan.prepare("text.glyph_lookup,world.fixed_budget,runtime.resource_string_cache,render.buffer_growth,render.frustum.positive-vertex",
                "mc189-optifine-m5", getClass().getClassLoader());
        assertEquals(5,plan.registrations().size());
        Set<String> targets=new HashSet<>();
        plan.registrations().forEach(r -> targets.addAll(r.targetClasses()));
        assertEquals(new HashSet<>(Arrays.asList("avn","bho","jy","bfd","bid")),targets);
        assertEquals(5,plan.patchPlan().selectedIds().size());
    }
    @Test void unknownDuplicateEmptyAndUnverifiedRuntimeSelectionsFail() {
        for(String selection:Arrays.asList("unknown","text.glyph_lookup,text.glyph_lookup","text.glyph_lookup,"))
            assertThrows(IllegalArgumentException.class,()->ManagedOptimPlan.prepare(selection,"mc189-optifine-m5",getClass().getClassLoader()));
        assertThrows(IllegalArgumentException.class,()->ManagedOptimPlan.prepare("text.glyph_lookup","other",getClass().getClassLoader()));
    }
    @Test void injectedHelpersMustBeVisibleThroughTheRealGameLoader() {
        ClassLoader missing=new ClassLoader(getClass().getClassLoader()) {
            @Override protected Class<?> loadClass(String name,boolean resolve) throws ClassNotFoundException {
                if(name.equals(GlyphIndex.class.getName()))throw new ClassNotFoundException(name);
                return super.loadClass(name,resolve);
            }
        };
        assertThrows(IllegalArgumentException.class,()->ManagedOptimPlan.prepare("text.glyph_lookup","mc189-optifine-m5",missing));
    }
}
