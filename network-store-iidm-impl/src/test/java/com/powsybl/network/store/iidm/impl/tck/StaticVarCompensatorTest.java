/**
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl.tck;

import com.powsybl.iidm.network.tck.AbstractStaticVarCompensatorTest;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class StaticVarCompensatorTest extends AbstractStaticVarCompensatorTest {

    @Override
    public void regulatingTerminalTest() {
        // FIXME delete this test when the regulating terminal is moved when the equipment regulated is deleted
    }
}
