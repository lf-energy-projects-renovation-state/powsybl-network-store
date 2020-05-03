/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.client;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.ConnectableType;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import com.powsybl.network.store.model.*;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.Pseudograph;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public abstract class AbstractTopology<T> {

    private static class EquipmentCount {
        int feederCount = 0;
        int branchCount = 0;
        int busbarSectionCount = 0;
    }

    private static <E> void countEquipments(EquipmentCount equipmentCount, Vertex<E> vertex) {
        switch (vertex.getConnectableType()) {
            case LINE:
            case TWO_WINDINGS_TRANSFORMER:
            case THREE_WINDINGS_TRANSFORMER:
            case HVDC_CONVERTER_STATION:
                equipmentCount.branchCount++;
                equipmentCount.feederCount++;
                break;

            case LOAD:
            case GENERATOR:
            case BATTERY:
            case SHUNT_COMPENSATOR:
            case DANGLING_LINE:
            case STATIC_VAR_COMPENSATOR:
                equipmentCount.feederCount++;
                break;

            case BUSBAR_SECTION:
                equipmentCount.busbarSectionCount++;
                break;

            default:
                throw new IllegalStateException();
        }
    }

    private static <E> boolean busViewBusValidator(Map<E, List<Vertex<E>>>verticesByNodeOrBus, Set<E> nodesOrBuses) {
        EquipmentCount equipmentCount = new EquipmentCount();
        for (E nodeOrBus : nodesOrBuses) {
            List<Vertex<E>> vertices = verticesByNodeOrBus.get(nodeOrBus);
            if (vertices != null) {
                for (Vertex<E> vertex : vertices) {
                    if (vertex != null) {
                        countEquipments(equipmentCount, vertex);
                    }
                }
            }
        }
        return (equipmentCount.busbarSectionCount >= 1 && equipmentCount.feederCount >= 1)
                || (equipmentCount.branchCount >= 1 && equipmentCount.feederCount >= 2);
    }

    protected abstract <U extends InjectionAttributes> T getInjectionNodeOrBus(Resource<U> resource);

    private <U extends InjectionAttributes> Vertex<T> createVertexFromInjection(Resource<U> resource) {
        ConnectableType connectableType;
        switch (resource.getType()) {
            case LOAD:
                connectableType = ConnectableType.LOAD;
                break;
            case GENERATOR:
                connectableType = ConnectableType.GENERATOR;
                break;
            case SHUNT_COMPENSATOR:
                connectableType = ConnectableType.SHUNT_COMPENSATOR;
                break;
            case VSC_CONVERTER_STATION:
            case LCC_CONVERTER_STATION:
                connectableType = ConnectableType.HVDC_CONVERTER_STATION;
                break;
            case STATIC_VAR_COMPENSATOR:
                connectableType = ConnectableType.STATIC_VAR_COMPENSATOR;
                break;
            case DANGLING_LINE:
                connectableType = ConnectableType.DANGLING_LINE;
                break;
            default:
                throw new IllegalStateException("Resource is not an injection: " + resource.getType());
        }
        T nodeOrBus = getInjectionNodeOrBus(resource);
        return nodeOrBus == null ? null : new Vertex<>(resource.getId(), connectableType, nodeOrBus, null);
    }

    protected abstract <U extends BranchAttributes> T getBranchNodeOrBus1(Resource<U> resource);

    protected abstract <U extends BranchAttributes> T getBranchNodeOrBus2(Resource<U> resource);

    private <U extends BranchAttributes> Vertex<T> createVertextFromBranch(Resource<U> resource, Resource<VoltageLevelAttributes> voltageLevelResource) {
        ConnectableType connectableType;
        switch (resource.getType()) {
            case LINE:
                connectableType = ConnectableType.LINE;
                break;
            case TWO_WINDINGS_TRANSFORMER:
                connectableType = ConnectableType.TWO_WINDINGS_TRANSFORMER;
                break;
            default:
                throw new IllegalStateException("Resource is not a branch: " + resource.getType());
        }
        T nodeOrBus;
        Branch.Side side;
        if (voltageLevelResource.getId().equals(resource.getAttributes().getVoltageLevelId1())) {
            nodeOrBus = getBranchNodeOrBus1(resource);
            side = Branch.Side.ONE;
        } else {
            nodeOrBus = getBranchNodeOrBus2(resource);
            side = Branch.Side.TWO;
        }
        return nodeOrBus == null ? null : new Vertex<>(resource.getId(), connectableType, nodeOrBus, side.name());
    }

    protected abstract <U extends ThreeWindingsTransformerAttributes> T get3wtNodeOrBus1(Resource<U> resource);

    protected abstract <U extends ThreeWindingsTransformerAttributes> T get3wtNodeOrBus2(Resource<U> resource);

    protected abstract <U extends ThreeWindingsTransformerAttributes> T get3wtNodeOrBus3(Resource<U> resource);

    private Vertex<T> createVertexFrom3wt(Resource<ThreeWindingsTransformerAttributes> resource, Resource<VoltageLevelAttributes> voltageLevelResource) {
        T nodeOrBus;
        ThreeWindingsTransformer.Side side;
        if (voltageLevelResource.getId().equals(resource.getAttributes().getLeg1().getVoltageLevelId())) {
            nodeOrBus = get3wtNodeOrBus1(resource);
            side = ThreeWindingsTransformer.Side.ONE;
        } else if (voltageLevelResource.getId().equals(resource.getAttributes().getLeg2().getVoltageLevelId())) {
            nodeOrBus = get3wtNodeOrBus2(resource);
            side = ThreeWindingsTransformer.Side.TWO;
        } else {
            nodeOrBus = get3wtNodeOrBus3(resource);
            side = ThreeWindingsTransformer.Side.THREE;
        }
        return nodeOrBus == null ? null : new Vertex<>(resource.getId(), ConnectableType.THREE_WINDINGS_TRANSFORMER, nodeOrBus, side.name());
    }

    protected void ensureNodeOrBusExists(UndirectedGraph<T, Resource<SwitchAttributes>> graph, T nodeOrBus) {
        if (!graph.containsVertex(nodeOrBus)) {
            graph.addVertex(nodeOrBus);
        }
    }

    public UndirectedGraph<T, Resource<SwitchAttributes>>  buildGraph(NetworkObjectIndex index, Resource<VoltageLevelAttributes> voltageLevelResource,
                                                                      Map<T, List<Vertex<T>>> verticesByNodeOrBus) {
        UndirectedGraph<T, Resource<SwitchAttributes>> graph = new Pseudograph<>((i, v1) -> {
            throw new IllegalStateException();
        });
        List<Vertex<T>> vertices = new ArrayList<>();
        buildGraph(index, voltageLevelResource, graph, vertices);
        verticesByNodeOrBus.putAll(vertices.stream().collect(Collectors.groupingBy(Vertex::getNodeOrBus)));
        return graph;
    }

    protected abstract <U extends SwitchAttributes> T getSwitchNodeOrBus1(Resource<U> resource);

    protected abstract <U extends SwitchAttributes> T getSwitchNodeOrBus2(Resource<U> resource);

    protected void buildGraph(NetworkObjectIndex index, Resource<VoltageLevelAttributes> voltageLevelResource,
                           UndirectedGraph<T, Resource<SwitchAttributes>> graph, List<Vertex<T>> vertices) {
        UUID networkUuid = index.getNetwork().getUuid();
        vertices.addAll(index.getStoreClient().getVoltageLevelGenerators(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelLoads(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelShuntCompensators(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelStaticVarCompensators(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelVscConverterStation(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelLccConverterStation(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelDanglingLines(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(this::createVertexFromInjection)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelLines(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(resource -> createVertextFromBranch(resource, voltageLevelResource))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelTwoWindingsTransformers(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(resource -> createVertextFromBranch(resource, voltageLevelResource))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        vertices.addAll(index.getStoreClient().getVoltageLevelThreeWindingsTransformers(networkUuid, voltageLevelResource.getId())
                .stream()
                .map(resource -> createVertexFrom3wt(resource, voltageLevelResource))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));

        for (Vertex<T> vertex : vertices) {
            graph.addVertex(vertex.getNodeOrBus());
        }

        for (Resource<SwitchAttributes> resource : index.getStoreClient().getVoltageLevelSwitches(networkUuid, voltageLevelResource.getId())) {
            if (!resource.getAttributes().isOpen()) {
                T nodeOrBus1 = getSwitchNodeOrBus1(resource);
                T nodeOrBus2 = getSwitchNodeOrBus2(resource);
                ensureNodeOrBusExists(graph, nodeOrBus1);
                ensureNodeOrBusExists(graph, nodeOrBus2);
                graph.addEdge(nodeOrBus1, nodeOrBus2, resource);
            }
        }
    }

    protected abstract CalculatedBus<T> createCalculatedBus(NetworkObjectIndex index,
                                                            Resource<VoltageLevelAttributes> voltageLevelResource,
                                                            List<Vertex<T>> vertices);

    private Optional<CalculatedBus<T>> tryToCreateCalculatedBus(NetworkObjectIndex index,
                                                                Resource<VoltageLevelAttributes> voltageLevelResource,
                                                                Map<T, List<Vertex<T>>> verticesByNodeOrBus,
                                                                Set<T> nodesOrBuses, // the component
                                                                BiPredicate<Map<T, List<Vertex<T>>>, Set<T>> busValidator) {
        // check that the component is a bus
        if (busValidator.test(verticesByNodeOrBus, nodesOrBuses)) {
            List<Vertex<T>> calculatedBusVertices = nodesOrBuses.stream()
                    .flatMap(nodeOrBus -> verticesByNodeOrBus.getOrDefault(nodeOrBus, Collections.emptyList()).stream())
                    .collect(Collectors.toList());
            return Optional.of(createCalculatedBus(index, voltageLevelResource, calculatedBusVertices));
        }
        return Optional.empty();
    }

    private Map<String, Bus> calculateBuses(NetworkObjectIndex index,
                                            Resource<VoltageLevelAttributes> voltageLevelResource,
                                            UndirectedGraph<T, Resource<SwitchAttributes>> graph,
                                            Map<T, List<Vertex<T>>> verticesByNodeOrBus,
                                            BiPredicate<Map<T, List<Vertex<T>>>, Set<T>> busValidator) {
        Map<String, Bus> calculatedBuses = new HashMap<>();
        for (Set<T> nodesOrBuses : new ConnectivityInspector<>(graph).connectedSets()) {
            tryToCreateCalculatedBus(index, voltageLevelResource, verticesByNodeOrBus, nodesOrBuses, busValidator)
                    .ifPresent(calculatedBus -> calculatedBuses.put(calculatedBus.getId(), calculatedBus));
        }
        return calculatedBuses;
    }

    public Map<String, Bus> calculateBuses(NetworkObjectIndex index, Resource<VoltageLevelAttributes> voltageLevelResource) {
        Map<T, List<Vertex<T>>> verticesByNodeOrBus = new HashMap<>();
        UndirectedGraph<T, Resource<SwitchAttributes>> graph = buildGraph(index, voltageLevelResource, verticesByNodeOrBus);
        return calculateBuses(index, voltageLevelResource, graph, verticesByNodeOrBus, AbstractTopology::busViewBusValidator);
    }

    private CalculatedBus<T> calculateBuses(NetworkObjectIndex index,
                                            Resource<VoltageLevelAttributes> voltageLevelResource,
                                            UndirectedGraph<T, Resource<SwitchAttributes>> graph,
                                            Map<T, List<Vertex<T>>> verticesByNodeOrBus,
                                            BiPredicate<Map<T, List<Vertex<T>>>, Set<T>> busValidator,
                                            T startNodeOrBus) {
        Set<T> nodesOrBuses = new ConnectivityInspector<>(graph).connectedSetOf(startNodeOrBus);
        return tryToCreateCalculatedBus(index, voltageLevelResource, verticesByNodeOrBus, nodesOrBuses, busValidator).orElse(null);
    }

    public CalculatedBus<T> calculateBuses(NetworkObjectIndex index, Resource<VoltageLevelAttributes> voltageLevelResource, T startNodeOrBus) {
        Map<T, List<Vertex<T>>> verticesByNodeOrBus = new HashMap<>();
        UndirectedGraph<T, Resource<SwitchAttributes>> graph = buildGraph(index, voltageLevelResource, verticesByNodeOrBus);
        return calculateBuses(index, voltageLevelResource, graph, verticesByNodeOrBus, AbstractTopology::busViewBusValidator, startNodeOrBus);
    }
}