package org.osgi.feature.builder;

import org.osgi.feature.Bundle;
import org.osgi.feature.Configuration;
import org.osgi.feature.MergeContext;

import java.util.List;
import java.util.function.BiFunction;

public class MergeContextBuilder {
    private BiFunction<Bundle, Bundle, List<Bundle>> bundleResolver;

    public MergeContextBuilder setBundleResolver(BiFunction<Bundle, Bundle, List<Bundle>> bf) {
        bundleResolver = bf;
        return this;
    }

    public MergeContext build() {
        return new MergeContextImpl(bundleResolver);
    }

    private static class MergeContextImpl implements MergeContext {
        private BiFunction<Bundle, Bundle, List<Bundle>> bundleResolver;

        private MergeContextImpl(BiFunction<Bundle, Bundle, List<Bundle>> bundleResolver) {
            this.bundleResolver = bundleResolver;
        }

        @Override
        public List<Bundle> resolveBundles(Bundle b1, Bundle b2) {
            return bundleResolver.apply(b1, b2);
        }

        @Override
        public Configuration resolveConfigurations(Configuration c1, Configuration c2) {
            // TODO Auto-generated method stub
            return null;
        }

    }
}
