/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.miele.internal.handler;

import static org.openhab.binding.miele.internal.MieleBindingConstants.APPLIANCE_ID;
import static org.openhab.binding.miele.internal.MieleBindingConstants.MIELE_DEVICE_CLASS_HOOD;

import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * The {@link HoodHandler} is responsible for handling commands,
 * which are sent to one of the channels
 *
 * @author Karel Goderis - Initial contribution
 * @author Kai Kreuzer - fixed handling of REFRESH commands
 * @author Martin Lepsy - fixed handling of empty JSON results
 * @author Jacob Laursen - Fixed multicast and protocol support (ZigBee/LAN)
 **/
public class HoodHandler extends MieleApplianceHandler<HoodChannelSelector> {

    private final Logger logger = LoggerFactory.getLogger(HoodHandler.class);

    public HoodHandler(Thing thing) {
        super(thing, HoodChannelSelector.class, MIELE_DEVICE_CLASS_HOOD);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        super.handleCommand(channelUID, command);

        String channelID = channelUID.getId();
        String applianceId = (String) getThing().getConfiguration().getProperties().get(APPLIANCE_ID);

        HoodChannelSelector selector = (HoodChannelSelector) getValueSelectorFromChannelID(channelID);
        JsonElement result = null;

        try {
            if (selector != null) {
                switch (selector) {
                    case LIGHT: {
                        if (command.equals(OnOffType.ON)) {
                            result = bridgeHandler.invokeOperation(applianceId, modelID, "startLighting");
                        } else if (command.equals(OnOffType.OFF)) {
                            result = bridgeHandler.invokeOperation(applianceId, modelID, "stopLighting");
                        }
                        break;
                    }
                    case STOP: {
                        if (command.equals(OnOffType.ON)) {
                            result = bridgeHandler.invokeOperation(applianceId, modelID, "stop");
                        }
                        break;
                    }
                    default: {
                        logger.debug("{} is a read-only channel that does not accept commands",
                                selector.getChannelID());
                    }
                }
            }
            // process result
            if (result != null && isResultProcessable(result)) {
                logger.debug("Result of operation is {}", result.getAsString());
            }
        } catch (IllegalArgumentException e) {
            logger.warn(
                    "An error occurred while trying to set the read-only variable associated with channel '{}' to '{}'",
                    channelID, command.toString());
        }
    }
}
