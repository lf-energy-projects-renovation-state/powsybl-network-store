/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl.tck;

import com.powsybl.iidm.network.tck.AbstractConvertTopologyTest;
import org.junit.jupiter.api.Test;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
class ConvertTopologyTest extends AbstractConvertTopologyTest {
    @Test
    public void testNodeBreakerToBusBreaker() {
        // FIXME : to be removed when VoltageLevel.convertToTopology will be implemented
    }

    @Test
    public void testNodeBreakerToBusBreakerOneElementDisconnected() {
        // FIXME : to be removed when VoltageLevel.convertToTopology will be implemented
    }

    @Test
    public void testNodeBreakerToBusBreakerWithArea() {
        // FIXME : to be removed when Area and VoltageLevel.convertToTopology will be implemented
    }
}
