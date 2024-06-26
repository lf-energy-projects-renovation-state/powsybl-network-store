/**
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.network.store.iidm.impl.extensions;

import com.powsybl.iidm.network.Terminal;
import com.powsybl.iidm.network.extensions.ReferencePriority;
import com.powsybl.network.store.iidm.impl.NetworkObjectIndex;
import com.powsybl.network.store.iidm.impl.TerminalRefUtils;
import com.powsybl.network.store.model.ReferencePriorityAttributes;

/**
 * @author Franck Lecuyer <franck.lecuyer at rte-france.com>
 */
class ReferencePriorityImpl implements ReferencePriority {

    private final ReferencePriorityAttributes attributes;
    private final NetworkObjectIndex index;

    public ReferencePriorityImpl(ReferencePriorityAttributes attributes, NetworkObjectIndex index) {
        this.attributes = attributes;
        this.index = index;
    }

    ReferencePriorityAttributes getAttributes() {
        return attributes;
    }

    @Override
    public Terminal getTerminal() {
        return TerminalRefUtils.getTerminal(index, attributes.getTerminal());
    }

    @Override
    public int getPriority() {
        return attributes.getPriority();
    }
}
