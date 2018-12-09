/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.hrznstudio.spark.mixin;

import com.hrznstudio.spark.BootstrapConfig;
import com.hrznstudio.spark.SparkLauncher;
import com.hrznstudio.spark.loader.TransformingClassLoader;
import com.hrznstudio.spark.plugin.ILaunchPlugin;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.lang.reflect.Method;

public class MixinLaunchPlugin implements ILaunchPlugin {
    private static final Logger LOGGER = LogManager.getLogger("spark-mixin");

    @Override
    public void acceptConfig(BootstrapConfig config) {
    }

    @Override
    public void acceptClassloader(TransformingClassLoader classLoader) {
        SparkLauncher.CLASS_LOADER.addLoadExemption("com.hrznstudio.spark.mixin.");
        MixinBootstrap.getPlatform().inject();
    }

    @Override
    public void launch(String[] strings) {
        gotoPhase(MixinEnvironment.Phase.DEFAULT);
    }

    private static void gotoPhase(MixinEnvironment.Phase phase) {
        try {
            Method gotoPhase = MixinEnvironment.class.getDeclaredMethod("gotoPhase", MixinEnvironment.Phase.class);
            gotoPhase.setAccessible(true);
            gotoPhase.invoke(null, phase);
        } catch (Throwable t) {
            LOGGER.error("Failed to switch mixin phase", t);
        }
    }
}
