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

import com.hrznstudio.spark.patch.PatchBlackboard;
import org.spongepowered.asm.service.IGlobalPropertyService;

public class GlobalBlackboard implements IGlobalPropertyService {
    @Override
    public final <T> T getProperty(String key) {
        return PatchBlackboard.<T>key(key).get();
    }

    @Override
    public final void setProperty(String key, Object value) {
        PatchBlackboard.key(key).set(value);
    }

    @Override
    public final <T> T getProperty(String key, T defaultValue) {
        T value = PatchBlackboard.<T>key(key).get();
        return value != null ? value : defaultValue;
    }

    @Override
    public final String getPropertyString(String key, String defaultValue) {
        Object value = PatchBlackboard.key(key).get();
        return value != null ? value.toString() : defaultValue;
    }
}
