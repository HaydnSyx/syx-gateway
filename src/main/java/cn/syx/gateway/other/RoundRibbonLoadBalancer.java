package cn.syx.gateway.other;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRibbonLoadBalancer<T> implements LoadBalancer<T> {

    AtomicInteger index = new AtomicInteger(0);

    @Override
    public T choose(List<T> providers) {
        if (Objects.isNull(providers) || providers.isEmpty()) {
            return null;
        }

        if (providers.size() == 1) {
            return providers.get(0);
        }

        return providers.get((index.getAndIncrement() & Integer.MAX_VALUE) % providers.size());
    }
}
