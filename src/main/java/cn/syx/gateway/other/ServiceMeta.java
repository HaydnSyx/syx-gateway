package cn.syx.gateway.other;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务描述
 *
 * @author sunyongxue
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceMeta {

    private String group;
    private String namespace;
    private String env;
    private String name;
    private String version;

    public String toPath() {
        return String.format("%s_%s_%s_%s_%s", namespace, env, group, name, version);
    }

    public String toMetas() {
        return String.format("%s_%s_%s_%s_%s", namespace, env, group, name, version);
    }
}
