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
package org.openhab.binding.deconz.internal.dto;

import java.util.Arrays;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link WindowCovering} is send by the websocket connection as well as the Rest API.
 * It is part of a {@link LightMessage}.
 *
 * This should be in sync with the supported lights from
 * https://github.com/dresden-elektronik/deconz-rest-plugin/wiki/Supported-Devices.
 *
 * @author Matthias Herrmann
 */
@NonNullByDefault
public class WindowCoveringState extends LightState {
    public @Nullable Boolean open;
    public @Nullable Boolean stop;
    public @Nullable Integer lift;
    public @Nullable Integer tilt;

    public void clear() {
        super.clear();
        open = null;
        stop = null;
        lift = null;
        tilt = null;
    }

    @Override
    public <A extends LightState> boolean equalsIgnoreNull(A o) {
        final boolean superEquals = super.equalsIgnoreNull(o);
        if(o instanceof WindowCoveringState) {
            final WindowCoveringState o2 = (WindowCoveringState) o;
            return superEquals && equalsIgnoreNull(open, o2.open) && equalsIgnoreNull(stop, o2.stop)
                && equalsIgnoreNull(lift, o2.lift) && equalsIgnoreNull(tilt, o2.tilt);
        } else {
            return superEquals;
        }
    }

    @Override
    public String toString() {
        return "WindowCoveringState [lift=" + lift + ", open=" + open + ", stop=" + stop + ", tilt=" + tilt +
            "reachable=" + reachable + ", on=" + on + ", bri=" + bri + ", alert='" + alert + '\'' + ", colormode='" + colormode +
            '\'' + ", effect='" + effect + '\'' + ", effectSpeed=" + effectSpeed + ", ontime=" + ontime + ", hue=" + hue +
            ", sat=" + sat + ", ct=" + ct + ", xy=" + Arrays.toString(xy) + ", transitiontime=" +
            transitiontime + "]"; }
}
