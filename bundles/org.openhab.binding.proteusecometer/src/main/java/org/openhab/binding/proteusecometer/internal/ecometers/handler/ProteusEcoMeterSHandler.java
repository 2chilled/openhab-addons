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
package org.openhab.binding.proteusecometer.internal.ecometers.handler;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.proteusecometer.internal.ProteusEcoMeterBindingConstants;
import org.openhab.binding.proteusecometer.internal.ProteusEcoMeterConfiguration;
import org.openhab.binding.proteusecometer.internal.ecometers.ProteusEcoMeterSReply;
import org.openhab.binding.proteusecometer.internal.ecometers.ProteusEcoMeterSService;
import org.openhab.binding.proteusecometer.internal.serialport.SerialPortService;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.MetricPrefix;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.library.unit.Units;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fazecast.jSerialComm.SerialPort;

/**
 * The {@link ProteusEcoMeterSHandler} updates thing channels when receiving data
 *
 * @author Matthias Herrmann - Initial contribution
 */
@NonNullByDefault
public class ProteusEcoMeterSHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(ProteusEcoMeterSHandler.class);
    private @Nullable SerialPort serialPort;
    private @Nullable ProteusEcoMeterConfiguration config;
    private @Nullable ScheduledFuture<?> job;

    public ProteusEcoMeterSHandler(final Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        config = getConfigAs(ProteusEcoMeterConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);
        job = scheduler.schedule(() -> {

            try {
                final ProteusEcoMeterSService ecoMeterSService = new ProteusEcoMeterSService();
                final Stream<ProteusEcoMeterSReply> replyStream = ecoMeterSService.read(config.usbPort,
                        serialPortService);
                updateStatus(ThingStatus.ONLINE);

                replyStream.forEach(reply -> {
                    updateState(ProteusEcoMeterBindingConstants.sensorLevel,
                            new QuantityType<>(reply.sensorLevelInCm, MetricPrefix.CENTI(SIUnits.METRE)));
                    updateState(ProteusEcoMeterBindingConstants.usableLevelInLitre,
                            new QuantityType<>(reply.usableLevelInLiter, Units.LITRE));
                    updateState(ProteusEcoMeterBindingConstants.usableLevelInPercent, new QuantityType<>(
                            100d / reply.totalCapacityInLiter * reply.usableLevelInLiter, Units.PERCENT));
                    updateState(ProteusEcoMeterBindingConstants.temperature,
                            new QuantityType<>(reply.tempInCelsius, SIUnits.CELSIUS));
                    updateState(ProteusEcoMeterBindingConstants.totalCapacity,
                            new QuantityType<>(reply.totalCapacityInLiter, Units.LITRE));
                });
                closeSerialPort();
            } catch (final Exception e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.GONE,
                        "Error reading from Port: " + e.getMessage());
                final Duration retryInitDelay = Duration.ofSeconds(10);
                logger.debug("Error communicating with eco meter s. Retrying in {}", retryInitDelay, e);
                closeSerialPort();
                job = scheduler.schedule(this::initialize, retryInitDelay.getSeconds(), TimeUnit.SECONDS);
            }
        }, 0, TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // at the moment there are no commands supported. The Eco Meter S would support configuration
        // commands, but this is not implemented yet
    }

    @Override
    public void dispose() {
        super.dispose();
        closeSerialPort();
        if (job != null) {
            job.cancel(true);
            job = null;
        }
    }

    private void closeSerialPort() {
        if (serialPort != null) {
            final boolean closed = serialPort.closePort();
            logger.debug("serialPort.closePort() returned {}", closed);
            serialPort = null;
        }
    }

    private SerialPortService serialPortService = new SerialPortService() {
        @NonNullByDefault
        public InputStream getInputStream(String portId, int baudRate, int numDataBits, int numStopBits, int parity) {

            try {
                ProteusEcoMeterSHandler.this.serialPort = SerialPort.getCommPort(portId);
                if (ProteusEcoMeterSHandler.this.serialPort == null) {
                    throw new IOException("SerialPort.getCommPort(" + portId + ") returned null");
                }
                ProteusEcoMeterSHandler.this.serialPort.closePort();

                ProteusEcoMeterSHandler.this.serialPort.setBaudRate(baudRate);
                ProteusEcoMeterSHandler.this.serialPort.setNumDataBits(numDataBits);
                ProteusEcoMeterSHandler.this.serialPort.setNumStopBits(numStopBits);
                ProteusEcoMeterSHandler.this.serialPort.setParity(parity);
                ProteusEcoMeterSHandler.this.serialPort.openPort();
                ProteusEcoMeterSHandler.this.serialPort
                        .setComPortTimeouts(com.fazecast.jSerialComm.SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 0, 0);
                final InputStream inputStream = ProteusEcoMeterSHandler.this.serialPort.getInputStream();
                if (inputStream == null) {
                    throw new Exception("serialPort.getInputStream() returned null");
                }
                return inputStream;
            } catch (final Exception e) {
                closeSerialPort();
                throw new RuntimeException(e);
            }
        }
    };
}
