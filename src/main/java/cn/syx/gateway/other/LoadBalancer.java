package cn.syx.gateway.other;

import java.util.List;

public interface LoadBalancer<T> {

    T choose(List<T> providers);
}
