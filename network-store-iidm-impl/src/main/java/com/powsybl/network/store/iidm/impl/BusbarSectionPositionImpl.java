/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl;

import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.extensions.BusbarSectionPosition;
import com.powsybl.network.store.model.BusbarSectionAttributes;
import com.powsybl.network.store.model.BusbarSectionPositionAttributes;
import com.powsybl.network.store.model.Resource;

/**
 * @author Jon Harper <jon.harper at rte-france.com>
 */
public class BusbarSectionPositionImpl extends AbstractExtension<BusbarSection> implements BusbarSectionPosition {

    public BusbarSectionPositionImpl(BusbarSectionImpl busbarSectionImpl) {
        super(busbarSectionImpl);
    }

    private static int checkIndex(int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Busbar index has to be greater or equals to zero");
        }
        return index;
    }

    private BusbarSectionImpl getBusbarSection() {
        return (BusbarSectionImpl) getExtendable();
    }

    private BusbarSectionPositionAttributes getPositionAttributes() {
        return getPositionAttributes(getBusbarSection().getResource());
    }

    @Override
    public int getBusbarIndex() {
        return getPositionAttributes().getBusbarIndex();
    }

    private BusbarSectionPositionAttributes getPositionAttributes(Resource<BusbarSectionAttributes> resource) {
        return resource.getAttributes().getPosition();
    }

    @Override
    public BusbarSectionPosition setBusbarIndex(int busbarIndex) {
        getBusbarSection().updateResource(res -> getPositionAttributes(res).setBusbarIndex(checkIndex(busbarIndex)));
        return this;
    }

    @Override
    public int getSectionIndex() {
        return getPositionAttributes().getSectionIndex();
    }

    @Override
    public BusbarSectionPosition setSectionIndex(int sectionIndex) {
        getBusbarSection().updateResource(res -> getPositionAttributes(res).setSectionIndex(checkIndex(sectionIndex)));
        return this;
    }
}
