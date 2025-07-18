/**
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.network.store.iidm.impl;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.NetworkListener;
import com.powsybl.iidm.network.VariantManager;
import com.powsybl.iidm.network.VariantManagerConstants;
import com.powsybl.network.store.model.VariantInfos;
import com.powsybl.network.store.model.utils.VariantUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class VariantManagerImpl implements VariantManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(VariantManagerImpl.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;

    private final NetworkObjectIndex index;

    public VariantManagerImpl(NetworkObjectIndex index) {
        this.index = index;
    }

    @Override
    public Collection<String> getVariantIds() {
        return index.getStoreClient().getVariantsInfos(index.getNetworkUuid()).stream()
                .map(VariantInfos::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public String getWorkingVariantId() {
        return index.getStoreClient().getVariantsInfos(index.getNetwork().getUuid()).stream()
                .filter(infos -> infos.getNum() == index.getWorkingVariantNum())
                .map(VariantInfos::getId)
                .findFirst()
                .orElseThrow();
    }

    @Override
    public void setWorkingVariant(String variantId) {
        int variantNum = VariantUtils.getVariantNum(variantId,
            index.getStoreClient().getVariantsInfos(index.getNetworkUuid()));
        index.setWorkingVariantNum(variantNum);
    }

    @Override
    public void cloneVariant(String sourceVariantId, List<String> targetVariantIds) {
        cloneVariant(sourceVariantId, targetVariantIds, false);
    }

    private void notifyVariantCreated(String sourceVariantId, String targetVariantId) {
        for (NetworkListener listener : index.getNetwork().getListeners()) {
            try {
                listener.onVariantCreated(sourceVariantId, targetVariantId);
            } catch (Exception t) {
                LOGGER.error(t.toString(), t);
            }
        }
    }

    private void notifyVariantOverwritten(String sourceVariantId, String targetVariantId) {
        for (NetworkListener listener : index.getNetwork().getListeners()) {
            try {
                listener.onVariantOverwritten(sourceVariantId, targetVariantId);
            } catch (Exception t) {
                LOGGER.error(t.toString(), t);
            }
        }
    }

    @Override
    public void cloneVariant(String sourceVariantId, List<String> targetVariantIds, boolean mayOverwrite) {
        Objects.requireNonNull(sourceVariantId);
        Objects.requireNonNull(targetVariantIds);
        if (targetVariantIds.isEmpty()) {
            throw new IllegalArgumentException("Empty target variant id list");
        }
        int workingVariantNum = index.getWorkingVariantNum();
        List<VariantInfos> variantsInfos = index.getStoreClient().getVariantsInfos(index.getNetworkUuid());
        int sourceVariantNum = VariantUtils.getVariantNum(sourceVariantId, variantsInfos);
        String workingVariantId = workingVariantNum != -1 ? getWorkingVariantId() : null;
        for (String targetVariantId : targetVariantIds) {
            boolean variantOverwritten = false;
            Optional<VariantInfos> targetVariant = VariantUtils.getVariant(targetVariantId, variantsInfos);
            if (targetVariant.isPresent()) {
                if (!mayOverwrite) {
                    throw new PowsyblException("Variant '" + targetVariantId + "' already exists");
                } else {
                    removeVariant(targetVariantId, false);
                    variantOverwritten = true;
                }
            }

            cloneVariantWithOptimisticConcurrency(variantsInfos, sourceVariantNum, targetVariantId);

            //If we overwrite the working variant we need to set back the working variant num because it's deleted in the removeVariant method
            if (targetVariantId.equals(workingVariantId)) {
                index.setWorkingVariantNum(workingVariantNum);
            }
            if (!variantOverwritten) {
                notifyVariantCreated(sourceVariantId, targetVariantId);
            } else {
                notifyVariantOverwritten(sourceVariantId, targetVariantId);
            }
        }
    }

    @Override
    public void cloneVariant(String sourceVariantId, String targetVariantId) {
        cloneVariant(sourceVariantId, Collections.singletonList(targetVariantId));
    }

    @Override
    public void cloneVariant(String sourceVariantId, String targetVariantId, boolean mayOverwrite) {
        cloneVariant(sourceVariantId, Collections.singletonList(targetVariantId), mayOverwrite);
    }

    private void cloneVariantWithOptimisticConcurrency(List<VariantInfos> variantsInfos, int sourceVariantNum, String targetVariantId) {
        List<VariantInfos> localVariantInfos = new ArrayList<>(variantsInfos);
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            int targetVariantNum = VariantUtils.findFirstAvailableVariantNum(localVariantInfos);
            LOGGER.debug("Trying to clone network {} with variantNum {} (attempt: {})", index.getNetworkUuid(), targetVariantNum, attempts + 1);
            try {
                index.getStoreClient().cloneNetwork(index.getNetworkUuid(), sourceVariantNum, targetVariantNum, targetVariantId);
                return;
            } catch (DuplicateVariantNumException e) {
                LOGGER.debug("Failed to clone network {} with variantNum {}", index.getNetworkUuid(), targetVariantNum);
                attempts++;
                if (attempts == MAX_RETRY_ATTEMPTS) {
                    throw new PowsyblException("Impossible to clone variant after " + attempts + " attempts", e);
                }
                localVariantInfos = index.getStoreClient().getVariantsInfos(index.getNetworkUuid(), true);
            }
        }
    }

    private void notifyVariantRemoved(String variantId) {
        for (NetworkListener listener : index.getNetwork().getListeners()) {
            try {
                listener.onVariantRemoved(variantId);
            } catch (Exception t) {
                LOGGER.error(t.toString(), t);
            }
        }
    }

    @Override
    public void removeVariant(String variantId) {
        removeVariant(variantId, true);
    }

    public void removeVariant(String variantId, boolean notifyRemoved) {
        if (VariantManagerConstants.INITIAL_VARIANT_ID.equals(variantId)) {
            throw new PowsyblException("Removing initial variant is forbidden");
        }
        int variantNum = VariantUtils.getVariantNum(variantId,
            index.getStoreClient().getVariantsInfos(index.getNetwork().getUuid()));
        index.getStoreClient().deleteNetwork(index.getNetwork().getUuid(), variantNum);
        if (notifyRemoved) {
            notifyVariantRemoved(variantId);
        }
        if (variantNum == index.getWorkingVariantNum()) {
            index.setWorkingVariantNum(-1);
        }
    }

    @Override
    public void allowVariantMultiThreadAccess(boolean allow) {
        throw new PowsyblException("Network store implementation does not support multi-thread access yet");
    }

    @Override
    public boolean isVariantMultiThreadAccessAllowed() {
        return false;
    }
}
