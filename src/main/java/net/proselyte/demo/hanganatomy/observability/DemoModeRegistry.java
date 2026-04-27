package net.proselyte.demo.hanganatomy.observability;

import java.util.concurrent.atomic.AtomicReference;

import net.proselyte.demo.hanganatomy.config.HangDemoProperties;
import org.springframework.stereotype.Component;

@Component
public class DemoModeRegistry {

    private final AtomicReference<DemoMode> currentMode;

    public DemoModeRegistry(HangDemoProperties properties) {
        this.currentMode = new AtomicReference<>(properties.demo().defaultMode());
    }

    public DemoMode currentMode() {
        return currentMode.get();
    }

    public DemoMode switchTo(DemoMode mode) {
        currentMode.set(mode);
        return mode;
    }
}
