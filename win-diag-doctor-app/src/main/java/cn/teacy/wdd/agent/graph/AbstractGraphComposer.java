package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.common.GraphInitializationException;
import cn.teacy.wdd.agent.common.GraphKeys;
import cn.teacy.wdd.agent.common.GraphNode;
import cn.teacy.wdd.agent.utils.GraphUtils;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public abstract class AbstractGraphComposer {

    private static final Set<Class<?>> VALID_NODE_TYPES = Set.of(
            AsyncNodeActionWithConfig.class,
            CompiledGraph.class,
            StateGraph.class
    );

    private final Map<Object, String> nodeInstanceToIdMap = new IdentityHashMap<>();

    protected CompiledGraph compose() {
        StateGraph builder = new StateGraph(GraphUtils.buildKeyStrategyFactory(
                this.determineGraphKeysClass()
        ));

        Set<String> registeredIds = new HashSet<>();
        StateGraph finalBuilder = beforeRegisterNodes(builder);

        try {
            ReflectionUtils.doWithFields(this.getClass(), field -> {
                String nodeId = this.idOfField(field);
                if (nodeId == null) return;

                if (registeredIds.contains(nodeId)) {
                    throw new GraphInitializationException("Duplicate Node ID detected: " + nodeId);
                }

                ReflectionUtils.makeAccessible(field);
                Object instance;
                try {
                    instance = field.get(this);
                } catch (IllegalAccessException e) {
                    throw new GraphInitializationException("Failed to access field: " + field.getName(), e);
                }

                if (instance == null) {
                    throw new GraphInitializationException("Field '" + field.getName() + "' is annotated with @GraphNode but value is null.");
                }

                if (nodeInstanceToIdMap.containsKey(instance)) {
                    throw new GraphInitializationException("Duplicate Node Instance detected for field: " + field.getName());
                }

                nodeInstanceToIdMap.put(instance, nodeId);
                registeredIds.add(nodeId);

                registerNode(finalBuilder, nodeId, instance);
            });

            addEdges(finalBuilder);

            builder = beforeCompile(finalBuilder);

            return builder.compile(compileConfig());

        } catch (GraphStateException e) {
            throw new GraphInitializationException("Graph construction failed", e);
        } catch (Exception e) {
            throw new GraphInitializationException("Unexpected error during graph composition", e);
        } finally {
            nodeInstanceToIdMap.clear();
        }

    }

    /**
     * 如果实例类型有效，则将其注册到图中
     * {@link AbstractGraphComposer#VALID_NODE_TYPES}
     * {@link AbstractGraphComposer#validType(Field)}
     */
    private void registerNode(StateGraph builder, String nodeId, Object instance) {
        try {
            if (instance instanceof AsyncNodeActionWithConfig node) {
                builder.addNode(nodeId, node);
            } else if (instance instanceof CompiledGraph compiledGraph) {
                builder.addNode(nodeId, compiledGraph);
            } else if (instance instanceof StateGraph stateGraph) {
                builder.addNode(nodeId, stateGraph);
            } else {
                // 如果通过了 validType 检查但这里无法匹配，说明 VALID_NODE_TYPES 和此处的 if-else 不一致
                throw new GraphInitializationException("Type " + instance.getClass().getName() + " is valid but handling logic is missing.");
            }
        } catch (GraphStateException e) {
            throw new GraphInitializationException("Failed to register node '" + nodeId + "': " + e.getMessage(), e);
        }
    }

    private boolean validType(Field field) {
        return VALID_NODE_TYPES.stream().anyMatch(type -> type.isAssignableFrom(field.getType()));
    }

    @Nullable
    private String idOfField(Field field) {
        if (field == null) {
            return null;
        }

        GraphNode annotation = field.getAnnotation(GraphNode.class);
        if (annotation == null) {
            return null;
        }

        if (!this.validType(field)) {
            log.warn("Field '{}' is annotated with @GraphNode but has invalid type: {}.",
                    field.getName(),
                    field.getType().getName()
            );
            return null;
        }

        return annotation.value();
    }

    /**
     * 获取指定节点实例对应的唯一 Node ID。
     * <p>
     * 该 ID 是通过解析字段上的 {@link GraphNode} 注解获取的。
     *
     * @param nodeInstance 节点对象实例（必须是当前类的成员变量且已初始化）
     * @return 该实例对应的 Graph Node ID
     * @throws GraphInitializationException 如果实例未注册或该方法在构建流程之外被调用
     *
     * @apiNote <strong>适用范围与生命周期说明：</strong>
     * <ul>
     * <li><strong>仅限构建期使用：</strong> 此方法仅设计用于 {@link #addEdges(StateGraph)} 或 {@link #beforeCompile(StateGraph)} 方法内部。</li>
     * <li><strong>临时性：</strong> 底层的实例映射表（Registry）是临时的。为了防止内存泄漏，它会在 {@link #compose()} 方法执行完毕后立即被 <strong>清空</strong>。</li>
     * <li><strong>禁止运行时调用：</strong> 请勿在图编译完成后的运行时逻辑中调用此方法，否则将抛出异常。</li>
     * </ul>
     */
    @NonNull
    protected String idOf(@NonNull Object nodeInstance) {
        String id = nodeInstanceToIdMap.get(nodeInstance);
        if (id == null) {
            throw new GraphInitializationException(
                    "Node instance [" + nodeInstance.getClass().getSimpleName() + "] not found in graph registry. "
            );
        }
        return id;
    }

    /**
     * 确定图的变量枚举类
     */
    @NonNull
    abstract protected Class<? extends GraphKeys> determineGraphKeysClass();

    abstract protected void addEdges(StateGraph builder) throws GraphStateException;

    abstract protected CompileConfig compileConfig();

    protected StateGraph beforeCompile(StateGraph builder) {
        return builder;
    }

    protected StateGraph beforeRegisterNodes(StateGraph builder) {
        return builder;
    }

}
