/**
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.model;

import com.powsybl.iidm.network.HvdcLine;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@Data
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "HVDC line attributes")
public class HvdcLineAttributes extends AbstractIdentifiableAttributes {

    @Schema(description = "HVDC line name")
    private String name;

    @Schema(description = "Resistance")
    private double r;

    @Schema(description = "Converter mode")
    private HvdcLine.ConvertersMode convertersMode;

    @Schema(description = "Nominal voltage")
    private double nominalV;

    @Schema(description = "Active power setpoint in MW")
    private double activePowerSetpoint;

    @Schema(description = "Maximum active power in MW")
    private double maxP;

    @Schema(description = "Side 1 converter station ID")
    private String converterStationId1;

    @Schema(description = "Side 2 converter station ID")
    private String converterStationId2;

    @Schema(description = "Hvdc angle droop active power control")
    private HvdcAngleDroopActivePowerControlAttributes hvdcAngleDroopActivePowerControl;

    @Schema(description = "Hvdc operator active power range")
    private HvdcOperatorActivePowerRangeAttributes hvdcOperatorActivePowerRange;
}
