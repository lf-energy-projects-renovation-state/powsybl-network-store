/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl.extensions;

import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreaAdder;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.Network;
import com.powsybl.network.store.iidm.impl.NetworkImpl;
import com.powsybl.network.store.model.CgmesControlAreaAttributes;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class CgmesControlAreasImpl extends AbstractExtension<Network> implements CgmesControlAreas {

    public CgmesControlAreasImpl(NetworkImpl network) {
        super(network);
    }

    private NetworkImpl getNetwork() {
        return (NetworkImpl) getExtendable();
    }

    @Override
    public CgmesControlAreaAdder newCgmesControlArea() {
        return new CgmesControlAreaAdderImpl(this, getNetwork());
    }

    @Override
    public Collection<CgmesControlArea> getCgmesControlAreas() {
        List<CgmesControlAreaAttributes> cgmesControlAreaAttributes = getNetwork()
                .getResource()
                .getAttributes()
                .getCgmesControlAreas()
                .getControlAreas();
        return IntStream.range(0, cgmesControlAreaAttributes.size())
                .boxed()
                .map(index -> new CgmesControlAreaImpl(this, getNetwork(), index))
                .collect(Collectors.toSet());
    }

    @Override
    public CgmesControlArea getCgmesControlArea(String controlAreaId) {
        Objects.requireNonNull(controlAreaId);
        List<CgmesControlAreaAttributes> cgmesControlAreaAttributes = getNetwork()
                .getResource()
                .getAttributes()
                .getCgmesControlAreas()
                .getControlAreas();
        return IntStream.range(0, cgmesControlAreaAttributes.size())
                .boxed()
                .map(index -> new CgmesControlAreaImpl(this, getNetwork(), index))
                .filter(area -> area.getId().equals(controlAreaId))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean containsCgmesControlAreaId(String controlAreaId) {
        Objects.requireNonNull(controlAreaId);
        return getNetwork()
                .getResource()
                .getAttributes()
                .getCgmesControlAreas()
                .getControlAreas()
                .stream()
                .anyMatch(a -> a.getId().equals(controlAreaId));
    }

    @Override
    public void cleanIfEmpty() {
    }
}
