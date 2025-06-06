/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl.extensions;

import com.powsybl.iidm.network.Connectable;
import com.powsybl.iidm.network.extensions.Measurements;
import com.powsybl.iidm.network.extensions.MeasurementsAdder;
import com.powsybl.network.store.iidm.impl.AbstractIdentifiableImpl;
import com.powsybl.network.store.model.MeasurementsAttributes;

/**
 * @author Abdelsalem Hedhili <abdelsalem.hedhili at rte-france.com>
 */
public class MeasurementsAdderImpl<C extends Connectable<C>> extends AbstractIidmExtensionAdder<C, Measurements<C>> implements MeasurementsAdder<C> {

    protected MeasurementsAdderImpl(C extendable) {
        super(extendable);
    }

    @Override
    protected Measurements<C> createExtension(C connectable) {
        MeasurementsAttributes measurementsAttributes = MeasurementsAttributes.builder()
                .build();
        ((AbstractIdentifiableImpl<?, ?>) connectable).updateResourceWithoutNotification(res -> res.getAttributes().getExtensionAttributes().put(Measurements.NAME, measurementsAttributes));
        return new MeasurementsImpl< >(connectable);
    }
}
