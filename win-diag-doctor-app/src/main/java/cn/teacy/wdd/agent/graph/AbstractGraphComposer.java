package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.common.GraphInitializationException;
import cn.teacy.wdd.agent.common.GraphKey;
import cn.teacy.wdd.agent.common.GraphNode;
import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@Slf4j
public abstract class AbstractGraphComposer {

    private static final Set<Class<?>> VALID_NODE_TYPES = Set.of(
            AsyncNodeActionWithConfig.class,
            CompiledGraph.class,
            StateGraph.class
    );

    private final Map<Object, String> nodeInstanceToIdMap = new IdentityHashMap<>();

    protected CompiledGraph compose() {

        Map<String, KeyStrategy> strategyMap = new HashMap<>();
        Map<String, Object> nodeMap = new LinkedHashMap<>();

        ReflectionUtils.doWithFields(this.getClass(), field -> {
            if (field.isAnnotationPresent(GraphKey.class)) {
                collectGraphKey(field, strategyMap);
            }
            else if (field.isAnnotationPresent(GraphNode.class)) {
                collectGraphNode(field, nodeMap);
            }
        });

        if (strategyMap.isEmpty()) {
            log.warn("No graph keys found in {}.", this.getClass().getSimpleName());
        }

        StateGraph builder = new StateGraph(() -> strategyMap);
        beforeRegisterNodes(builder);
        nodeMap.forEach((nodeId, instance) -> registerNode(builder, nodeId, instance));

        try {
            addEdges(builder);

            beforeCompile(builder);

            return builder.compile(compileConfig());

        } catch (GraphStateException e) {
            throw new GraphInitializationException("Graph construction failed", e);
        } catch (Exception e) {
            throw new GraphInitializationException("Unexpected error during graph composition", e);
        } finally {
            nodeInstanceToIdMap.clear();
        }

    }

    private void collectGraphKey(Field field, Map<String, KeyStrategy> strategyMap) {
        int modifiers = field.getModifiers();
        boolean isValidConstant = Modifier.isStatic(modifiers)
                && Modifier.isFinal(modifiers)
                && field.getType().equals(String.class);

        if (!isValidConstant) {
            throw new GraphInitializationException(
                    "Field [" + field.getName() + "] is annotated with @GraphKey but is not 'static final String'."
            );
        }

        try {
            ReflectionUtils.makeAccessible(field);
            String keyName = (String) field.get(null);
            if (keyName == null || keyName.isBlank()) {
                throw new GraphInitializationException("Graph key value cannot be empty: " + field.getName());
            }

            if (!strategyMap.containsKey(keyName)) {
                GraphKey annotation = field.getAnnotation(GraphKey.class);
                KeyStrategy strategy = annotation.strategy().getDeclaredConstructor().newInstance();
                strategyMap.putIfAbsent(keyName, strategy); // subclass prior to superclass
            }
        } catch (Exception e) {
            throw new GraphInitializationException("Failed to process GraphKey field: " + field.getName(), e);
        }
    }

    private void collectGraphNode(Field field, Map<String, Object> nodeMap) {
        String nodeId = field.getAnnotation(GraphNode.class).value();
        if (nodeMap.containsKey(nodeId)) {
            throw new GraphInitializationException("Duplicate Node ID detected: " + nodeId);
        }

        if (VALID_NODE_TYPES.stream().noneMatch(t -> t.isAssignableFrom(field.getType()))) {
            log.warn("Field '{}' is annotated with @GraphNode but has invalid type.", field.getName());
            return;
        }

        ReflectionUtils.makeAccessible(field);
        try {
            Object instance = field.get(this);
            if (instance == null) {
                throw new GraphInitializationException("Node field '" + field.getName() + "' is null.");
            }

            if (nodeInstanceToIdMap.containsKey(instance)) {
                throw new GraphInitializationException("Duplicate Node Instance detected: " + field.getName());
            }

            nodeMap.put(nodeId, instance);
            nodeInstanceToIdMap.put(instance, nodeId);

        } catch (IllegalAccessException e) {
            throw new GraphInitializationException("Failed to access node field: " + field.getName(), e);
        }
    }

    /**
     * 如果实例类型有效，则将其注册到图中
     * {@link AbstractGraphComposer#VALID_NODE_TYPES}
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

    abstract protected void addEdges(StateGraph builder) throws GraphStateException;

    abstract protected CompileConfig compileConfig();

    protected void beforeCompile(StateGraph builder) {
    }

    protected void beforeRegisterNodes(StateGraph builder) {
    }

}
