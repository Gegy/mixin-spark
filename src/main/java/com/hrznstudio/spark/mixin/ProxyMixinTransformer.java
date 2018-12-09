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

import com.hrznstudio.spark.patch.IBytePatcher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.service.ILegacyClassTransformer;

import java.lang.reflect.Constructor;

public final class ProxyMixinTransformer implements IBytePatcher, ILegacyClassTransformer {
    private static final Logger LOGGER = LogManager.getLogger("spark-mixin");

    private static final ILegacyClassTransformer TRANSFORMER = constructTransformer();

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public byte[] apply(String name, byte[] bytes) {
        return ProxyMixinTransformer.TRANSFORMER.transformClassBytes(name, name, bytes);
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] bytes) {
        return ProxyMixinTransformer.TRANSFORMER.transformClassBytes(name, transformedName, bytes);
    }

    private static ILegacyClassTransformer constructTransformer() {
        try {
            Constructor<MixinTransformer> constructor = MixinTransformer.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Throwable t) {
            LOGGER.error("Failed to construct mixin transformer", t);
        }
        return new VoidLegacyTransformer();
    }
}
