/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl.extensions;

import com.powsybl.network.store.iidm.impl.BusbarSectionImpl;
import com.powsybl.iidm.network.BusbarSection;
import com.powsybl.iidm.network.extensions.BusbarSectionPosition;
import com.powsybl.iidm.network.extensions.BusbarSectionPositionAdder;
import com.powsybl.network.store.model.BusbarSectionPositionAttributes;

/**
 * @author Jon Harper <jon.harper at rte-france.com>
 */
public class BusbarSectionPositionAdderImpl extends AbstractIidmExtensionAdder<BusbarSection, BusbarSectionPosition>
        implements BusbarSectionPositionAdder {

    private int busbarIndex = -1;
    private int sectionIndex = -1;

    protected BusbarSectionPositionAdderImpl(BusbarSection extendable) {
        super(extendable);
    }

    @Override
    public BusbarSectionPositionAdder withBusbarIndex(int busbarIndex) {
        this.busbarIndex = busbarIndex;
        return this;
    }

    @Override
    public BusbarSectionPositionAdder withSectionIndex(int sectionIndex) {
        this.sectionIndex = sectionIndex;
        return this;
    }

    @Override
    protected BusbarSectionPosition createExtension(BusbarSection busbarSection) {
        BusbarSectionImpl busbarSectionImpl = (BusbarSectionImpl) busbarSection;
        BusbarSectionPositionAttributes bspa = BusbarSectionPositionAttributes.builder()
                .busbarIndex(busbarIndex)
                .sectionIndex(sectionIndex)
                .build();
        busbarSectionImpl.updateResourceWithoutNotification(res -> res.getAttributes().setPosition(bspa));
        return new BusbarSectionPositionImpl(busbarSectionImpl);
    }

}
