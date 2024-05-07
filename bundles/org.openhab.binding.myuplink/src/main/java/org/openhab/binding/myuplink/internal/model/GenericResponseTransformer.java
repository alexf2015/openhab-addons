/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.myuplink.internal.model;

import static org.openhab.binding.myuplink.internal.MyUplinkBindingConstants.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.myuplink.internal.Utils;
import org.openhab.binding.myuplink.internal.handler.DynamicChannelProvider;
import org.openhab.core.thing.Channel;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.binding.builder.ChannelBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * transforms the http response into the openhab datamodel (instances of State)
 * this is a generic trnasformer which tries to map json fields 1:1 to channels.
 *
 * @author Alexander Friese - initial contribution
 */
@NonNullByDefault
public class GenericResponseTransformer {
    private final Logger logger = LoggerFactory.getLogger(GenericResponseTransformer.class);
    private final DynamicChannelProvider channelProvider;
    // TODO: private final CustomResponseTransformer customResponseTransformer;

    public GenericResponseTransformer(DynamicChannelProvider channelProvider) {
        this.channelProvider = channelProvider;
        // TODO: this.customResponseTransformer = new CustomResponseTransformer(channelProvider);
    }

    public Map<Channel, State> transform(JsonObject jsonData, String group) {
        Map<Channel, State> result = new HashMap<>(20);

        for (JsonElement channelData : Utils.getAsJsonArray(jsonData, JSON_KEY_ROOT_DATA)) {

            logger.debug(channelData.toString());

            String value = Utils.getAsString(channelData.getAsJsonObject(), "value");
            String channelId = Utils.getAsString(channelData.getAsJsonObject(), "parameterId");
            channelId = channelId == null ? "" : channelId;

            Channel channel = channelProvider.getChannel(group, channelId == null ? "" : channelId);
            if (channel == null) {
                logger.debug("Channel not found: {}#{}, will be created.", group, channelId);
                channel = createChannel(channelId, channelData.getAsJsonObject());
            } else {
                logger.debug("mapping value '{}' to channel {}", value, channel.getUID().getId());
                String channelType = channel.getAcceptedItemType();
            }
            // if (value == null || channelType == null) {
            // result.put(channel, UnDefType.NULL);
            // } else {
            // try {
            // String channelTypeId = Utils.getChannelTypeId(channel);
            // switch (channelType) {
            // case CHANNEL_TYPE_SWITCH:
            // result.put(channel, OnOffType.from(Boolean.parseBoolean(value)));
            // break;
            // case CHANNEL_TYPE_VOLT:
            // result.put(channel, new QuantityType<>(Double.parseDouble(value), Units.VOLT));
            // break;
            // case CHANNEL_TYPE_AMPERE:
            // result.put(channel, new QuantityType<>(Double.parseDouble(value), Units.AMPERE));
            // break;
            // case CHANNEL_TYPE_KWH:
            // result.put(channel, new QuantityType<>(Double.parseDouble(value),
            // MetricPrefix.KILO(Units.WATT_HOUR)));
            // break;
            // case CHANNEL_TYPE_POWER:
            // result.put(channel,
            // new QuantityType<>(Double.parseDouble(value), MetricPrefix.KILO(Units.WATT)));
            // break;
            // case CHANNEL_TYPE_DATE:
            // result.put(channel, new DateTimeType(Utils.parseDate(value)));
            // break;
            // case CHANNEL_TYPE_STRING:
            // result.put(channel, new StringType(value));
            // break;
            // case CHANNEL_TYPE_NUMBER:
            // if (channelTypeId.contains(CHANNEL_TYPENAME_INTEGER)) {
            // // explicit type long is needed in case of integer/long values otherwise automatic
            // // transformation to a decimal type is applied.
            // result.put(channel, new DecimalType(Long.parseLong(value)));
            // } else {
            // result.put(channel, new DecimalType(Double.parseDouble(value)));
            // }
            // break;
            // default:
            // logger.warn("no mapping implemented for channel type '{}'", channelType);
            // }

            // // call the custom handler to handle specific / composite channels which do not map 1:1 to JSON
            // // fields.
            // // TODO: result.putAll(customResponseTransformer.transform(channel, value, jsonData));

            // } catch (NumberFormatException | DateTimeParseException ex) {
            // logger.warn("caught exception while parsing data for channel {} (value '{}'). Exception: {}",
            // channel.getUID().getId(), value, ex.getMessage());
            // }
            // }
            // }
        }

        return result;
    }

    private Channel createChannel(String channelId, JsonObject channelData) {
        String label = Utils.getAsString(channelData.getAsJsonObject(), JSON_KEY_CHANNEL_LABEL);
        label = label == null ? "" : label;
        String unit = Utils.getAsString(channelData.getAsJsonObject(), JSON_KEY_CHANNEL_UNIT);
        unit = unit == null ? "" : unit;

        String channelType = switch (unit) {
            case JSON_VAL_UNIT_TEMPERATURE -> CHANNEL_TYPENAME_TEMPERATURE;
            case JSON_VAL_UNIT_ENERGY -> CHANNEL_TYPENAME_ENERGY;
            default -> CHANNEL_TYPENAME_DEFAULT;
        };

        ChannelUID channelUID = channelProvider.createChannelUID(channelId);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(BINDING_ID, channelType);
        Channel newChannel = ChannelBuilder.create(channelUID).withLabel(label).withDescription(label)
                .withType(channelTypeUID).build();

        channelProvider.addDynamicChannel(newChannel);
        return newChannel;
    }
}
