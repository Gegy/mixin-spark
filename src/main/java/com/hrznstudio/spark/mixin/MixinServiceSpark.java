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

import com.google.common.collect.ImmutableList;
import com.hrznstudio.spark.SparkLauncher;
import com.hrznstudio.spark.transformer.IByteTransformer;
import org.apache.commons.io.IOUtils;
import org.spongepowered.asm.lib.ClassReader;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.IMixinService;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.util.ReEntranceLock;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MixinServiceSpark implements IMixinService, IClassProvider, IClassBytecodeProvider {
    private final ReEntranceLock lock = new ReEntranceLock(1);

    @Override
    public String getName() {
        return "JarSpark";
    }

    @Override
    public boolean isValid() {
        try {
            Class.forName("com.hrznstudio.spark.SparkLauncher");
        } catch (Throwable t) {
            return false;
        }
        return true;
    }

    @Override
    public void prepare() {
        SparkLauncher.CLASS_LOADER.addLoadExemption("com.hrznstudio.spark.mixin.");
    }

    @Override
    public Phase getInitialPhase() {
        return Phase.INIT;
    }

    @Override
    public void init() {
    }

    @Override
    public ReEntranceLock getReEntranceLock() {
        return this.lock;
    }

    @Override
    public Collection<String> getPlatformAgents() {
        return ImmutableList.of();
    }

    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return SparkLauncher.CLASS_LOADER.findClass(name);
    }

    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, SparkLauncher.CLASS_LOADER);
    }

    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, SparkLauncher.class.getClassLoader());
    }

    @Override
    public void beginPhase() {
    }

    @Override
    public void checkEnv(Object bootSource) {
        if (bootSource.getClass().getClassLoader() != SparkLauncher.CLASS_LOADER) {
            throw new MixinException("Attempted to init the mixin environment in the wrong classloader");
        }
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return SparkLauncher.CLASS_LOADER.getResourceAsStream(name);
    }

    @Override
    public void registerInvalidClass(String className) {
        SparkLauncher.CLASS_LOADER.invalidate(className, new RuntimeException("Invalid"));
    }

    @Override
    public boolean isClassLoaded(String className) {
        return SparkLauncher.CLASS_LOADER.isLoaded(className);
    }

    @Override
    public String getClassRestrictions(String className) {
        List<String> restrictions = new ArrayList<>();
        if (SparkLauncher.CLASS_LOADER.isLoadExempt(className)) {
            restrictions.add("PACKAGE_CLASSLOADER_EXCLUSION");
        }
        if (SparkLauncher.CLASS_LOADER.isTransformExempt(className)) {
            restrictions.add("PACKAGE_TRANSFORMER_EXCLUSION");
        }
        return String.join(",", restrictions);
    }

    @Override
    public URL[] getClassPath() {
        return SparkLauncher.CLASS_LOADER.getURLs();
    }

    @Override
    public Collection<ITransformer> getTransformers() {
        Collection<IByteTransformer> transformers = SparkLauncher.ROSTER.getTransformers();
        List<ITransformer> wrapped = new ArrayList<>(transformers.size());
        for (IByteTransformer transformer : transformers) {
            if (transformer instanceof ITransformer) {
                wrapped.add((ITransformer) transformer);
            } else {
                wrapped.add(new WrapperLegacyTransformer(transformer));
            }
        }
        return wrapped;
    }

    @Override
    public byte[] getClassBytes(String name, String transformedName) throws IOException {
        byte[] bytes = SparkLauncher.CLASS_LOADER.readClassBytes(name);
        if (bytes != null) {
            return bytes;
        }

        URLClassLoader appClassLoader = (URLClassLoader) SparkLauncher.class.getClassLoader();

        String resourcePath = transformedName.replace('.', '/') + ".class";
        try (InputStream classStream = appClassLoader.getResourceAsStream(resourcePath)) {
            return IOUtils.toByteArray(classStream);
        } catch (Exception ex) {
            return null;
        }
    }

    @Override
    public byte[] getClassBytes(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        String name = className.replace('/', '.');

        Profiler profiler = MixinEnvironment.getProfiler();
        Section loadTime = profiler.begin(Profiler.ROOT, "class.load");
        byte[] classBytes = this.getClassBytes(name, name);
        loadTime.end();

        if (runTransformers) {
            Section transformTime = profiler.begin(Profiler.ROOT, "class.transform");
            classBytes = this.applyTransformers(name, classBytes, profiler);
            transformTime.end();
        }

        if (classBytes == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", name));
        }

        return classBytes;
    }

    private byte[] applyTransformers(String name, byte[] basicClass, Profiler profiler) {
        if (SparkLauncher.CLASS_LOADER.isTransformExempt(name)) {
            return basicClass;
        }

        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();

        for (ILegacyClassTransformer transformer : environment.getTransformers()) {
            this.lock.clear();

            int pos = transformer.getName().lastIndexOf('.');
            String simpleName = transformer.getName().substring(pos + 1);
            Section transformTime = profiler.begin(Profiler.FINE, simpleName.toLowerCase());
            transformTime.setInfo(transformer.getName());
            basicClass = transformer.transformClassBytes(name, name, basicClass);
            transformTime.end();

            if (this.lock.isSet()) {
                environment.addTransformerExclusion(transformer.getName());
                this.lock.clear();
            }
        }

        return basicClass;
    }

    @Override
    public ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return this.parseClass(this.getClassBytes(className, true), 0);
    }

    private ClassNode parseClass(byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(classBytes);
        classReader.accept(classNode, flags);
        return classNode;
    }

    @Override
    public final String getSideName() {
        return "CLIENT";
    }
}
