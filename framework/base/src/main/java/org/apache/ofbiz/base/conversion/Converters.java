/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package org.apache.ofbiz.base.conversion;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ofbiz.base.lang.SourceMonitored;
import org.apache.ofbiz.base.util.Debug;
import org.apache.ofbiz.base.util.UtilGenerics;

/** A <code>Converter</code> factory and repository. */
@SourceMonitored
public final class Converters {
    private static final String MODULE = Converters.class.getName();
    private static final String DELIMITER = "->";
    private static final ConcurrentHashMap<String, Converter<?, ?>> CONVERTER_MAP = new ConcurrentHashMap<>();
    private static final Set<ConverterCreator> CREATORS = new HashSet<>();
    private static final Set<String> NO_CONVERSIONS = new HashSet<>();

    static {
        registerCreator(new PassThruConverterCreator());
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Iterator<ConverterLoader> converterLoaders = ServiceLoader.load(ConverterLoader.class, loader).iterator();
        while (converterLoaders.hasNext()) {
            try {
                ConverterLoader converterLoader = converterLoaders.next();
                converterLoader.loadConverters();
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }
    }

    private Converters() { }

    /** Returns an appropriate <code>Converter</code> instance for
     * <code>sourceClass</code> and <code>targetClass</code>. If no matching
     * <code>Converter</code> is found, the method throws
     * <code>ClassNotFoundException</code>.
     * <p>This method is intended to be used when the source or
     * target <code>Object</code> types are unknown at compile time.
     * If the source and target <code>Object</code> types are known
     * at compile time, then one of the "ready made" converters should be used.
     *
     * @param sourceClass The object class to convert from
     * @param targetClass The object class to convert to
     * @return A matching <code>Converter</code> instance
     * @throws ClassNotFoundException
     */
    public static <S, T> Converter<S, T> getConverter(Class<S> sourceClass, Class<T> targetClass) throws ClassNotFoundException {
        String key = sourceClass.getName().concat(DELIMITER).concat(targetClass.getName());
        if (Debug.verboseOn()) {
            Debug.logVerbose("Getting converter: " + key, MODULE);
        }
        OUTER:
        do {
            Converter<?, ?> result = CONVERTER_MAP.get(key);
            if (result != null) {
                return UtilGenerics.cast(result);
            }
            if (NO_CONVERSIONS.contains(key)) {
                throw new ClassNotFoundException("No converter found for " + key);
            }
            Class<?> foundSourceClass = null;
            Converter<?, ?> foundConverter = null;
            for (Converter<?, ?> value : CONVERTER_MAP.values()) {
                if (value.canConvert(sourceClass, targetClass)) {
                    // this converter can deal with the source/target pair
                    if (foundSourceClass == null || foundSourceClass.isAssignableFrom(value.getSourceClass())) {
                        // remember the current target source class; if we find another converter, check
                        // to see if it's source class is assignable to this one, and if so, it means it's
                        // a child class, so we'll then take that converter.
                        foundSourceClass = value.getSourceClass();
                        foundConverter = value;
                    }
                }
            }
            if (foundConverter != null) {
                CONVERTER_MAP.putIfAbsent(key, foundConverter);
                continue OUTER;
            }
            for (ConverterCreator value : CREATORS) {
                result = createConverter(value, sourceClass, targetClass);
                if (result != null) {
                    CONVERTER_MAP.putIfAbsent(key, result);
                    continue OUTER;
                }
            }
            boolean addedToSet = false;
            synchronized (NO_CONVERSIONS) {
                addedToSet = NO_CONVERSIONS.add(key);
            }
            if (addedToSet) {
                Debug.logWarning("*** No converter found, converting from "
                        + sourceClass.getName() + " to " + targetClass.getName()
                        + ". Please report this message to the developer community so "
                        + "a suitable converter can be created. ***", MODULE);
            }
            throw new ClassNotFoundException("No converter found for " + key);
        } while (true);
    }

    private static <S, SS extends S, T, TT extends T> Converter<SS, TT> createConverter(ConverterCreator creater, Class<SS> sourceClass,
                                                                                        Class<TT> targetClass) {
        return creater.createConverter(sourceClass, targetClass);
    }

    /** Load all classes that implement <code>Converter</code> and are
     * contained in <code>containerClass</code>.
     * @param containerClass
     */
    public static void loadContainedConverters(Class<?> containerClass) {
        // This only returns -public- classes and interfaces
        for (Class<?> clz: containerClass.getClasses()) {
            try {
                // non-abstract, which means no interfaces or abstract classes
                if ((clz.getModifiers() & Modifier.ABSTRACT) == 0) {
                    Object value;
                    try {
                        value = clz.getConstructor().newInstance();
                    } catch (NoSuchMethodException e) {
                        // ignore this, as this class might be some other helper class,
                        // with a non-pubilc constructor
                        continue;
                    }
                    if (value instanceof ConverterLoader) {
                        ConverterLoader loader = (ConverterLoader) value;
                        loader.loadConverters();
                    }
                }
            } catch (Exception e) {
                Debug.logError(e, MODULE);
            }
        }
    }

    /** Registers a <code>ConverterCreater</code> instance to be used by the
     * {@link org.apache.ofbiz.base.conversion.Converters#getConverter(Class, Class)}
     * method, when a converter can't be found.
     * @param <S> The source object type
     * @param <T> The target object type
     * @param creator The <code>ConverterCreater</code> instance to register
     */
    static <S, T> void registerCreator(ConverterCreator creator) {
        synchronized (CREATORS) {
            CREATORS.add(creator);
        }
    }

    /** Registers a <code>Converter</code> instance to be used by the
     * {@link org.apache.ofbiz.base.conversion.Converters#getConverter(Class, Class)}
     * method.
     * @param <S> The source object type
     * @param <T> The target object type
     * @param converter The <code>Converter</code> instance to register
     */
    public static <S, T> void registerConverter(Converter<S, T> converter) {
        registerConverter(converter, converter.getSourceClass(), converter.getTargetClass());
    }

    private static <S, T> void registerConverter(Converter<S, T> converter, Class<?> sourceClass, Class<?> targetClass) {
        StringBuilder sb = new StringBuilder();
        if (sourceClass != null) {
            sb.append(sourceClass.getName());
        } else {
            sb.append("<null>");
        }
        sb.append(DELIMITER);
        sb.append(targetClass.getName());
        String key = sb.toString();
        if (CONVERTER_MAP.putIfAbsent(key, converter) == null) {
            if (Debug.verboseOn()) {
                Debug.logVerbose("Registered converter " + converter.getClass().getName(), MODULE);
            }
        }
    }

    protected static class PassThruConverterCreator implements ConverterCreator {
        PassThruConverterCreator() {
        }

        @Override
        public <S, T> Converter<S, T> createConverter(Class<S> sourceClass, Class<T> targetClass) {
            if (targetClass.isAssignableFrom(sourceClass)) {
                return new PassThruConverter<>(sourceClass, targetClass);
            }
            return null;
        }
    }

    /** Pass thru converter used when the source and target java object
     * types are the same. The <code>convert</code> method returns the
     * source object.
     */
    protected static class PassThruConverter<S, T> implements Converter<S, T> {
        private final Class<S> sourceClass;
        private final Class<T> targetClass;

        PassThruConverter(Class<S> sourceClass, Class<T> targetClass) {
            this.sourceClass = sourceClass;
            this.targetClass = targetClass;
        }

        @Override
        public boolean canConvert(Class<?> sourceClass, Class<?> targetClass) {
            return this.sourceClass == sourceClass && this.targetClass == targetClass;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T convert(S obj) throws ConversionException {
            return (T) obj;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T convert(Class<? extends T> targetClass, S obj) throws ConversionException {
            return (T) obj;
        }

        @Override
        public Class<?> getSourceClass() {
            return sourceClass;
        }

        @Override
        public Class<?> getTargetClass() {
            return targetClass;
        }
    }
}
