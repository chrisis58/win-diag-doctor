package cn.teacy.wdd.agent.graph;

import cn.teacy.wdd.agent.common.GraphInitializationException;
import cn.teacy.wdd.agent.common.GraphKeys;
import cn.teacy.wdd.agent.common.GraphNode;
import cn.teacy.wdd.agent.utils.GraphUtils;
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

    protected StateGraph initGraphBuilder() {
        StateGraph builder = new StateGraph(GraphUtils.buildKeyStrategyFactory(
                this.determineGraphKeysClass()
        ));

        Set<String> registeredIds = new HashSet<>();

        ReflectionUtils.doWithFields(this.getClass(), field -> {
            String nodeId = this.idOfField(field);
            if (nodeId == null) {
                return;
            }

            if (registeredIds.contains(nodeId)) {
                throw new GraphInitializationException("Duplicate Node ID detected: " + nodeId);
            }

            try {
                ReflectionUtils.makeAccessible(field);
                Object instance = field.get(this);

                if (instance == null) {
                    throw new GraphInitializationException("Field '" + field.getName() + "' is annotated with @GraphNode but value is null.");
                }

                if (nodeInstanceToIdMap.containsKey(instance)) {
                    throw new GraphInitializationException("Duplicate Node Instance detected for field: " + field.getName());
                }

                nodeInstanceToIdMap.put(instance, nodeId);
                registeredIds.add(nodeId);
                registerNode(builder, nodeId, instance);

            } catch (IllegalAccessException e) {
                throw new GraphInitializationException("Failed to access field: " + field.getName(), e);
            } catch (GraphStateException e) {
                throw new GraphInitializationException("Failed to register node into graph builder: " + nodeId, e);
            }
        });

        try {
            addEdges(builder);
        } catch (GraphStateException e) {
            log.error("Failed to add edges: {}", e.getMessage());
            throw new GraphInitializationException("Failed to add edges");
        } finally {
            nodeInstanceToIdMap.clear();
        }
        return builder;
    }

    /**
     * 如果实例类型有效，则将其注册到图中
     * {@link AbstractGraphComposer#VALID_NODE_TYPES}
     * {@link AbstractGraphComposer#validType(Field)}
     */
    private void registerNode(StateGraph builder, String nodeId, Object instance) throws GraphStateException {
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

    @NonNull
    protected String idOf(Object nodeInstance) {
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
    abstract Class<? extends GraphKeys> determineGraphKeysClass();

    abstract void addEdges(StateGraph builder) throws GraphStateException;

}
