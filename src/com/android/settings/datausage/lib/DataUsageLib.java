/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.datausage.lib;

import android.content.Context;
import android.net.NetworkStats;
import android.net.NetworkTemplate;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.android.internal.util.ArrayUtils;

import java.util.List;
import java.util.Set;

/**
 * Lib class for data usage
 */
public class DataUsageLib {
    private static final String TAG = "DataUsageLib";

    /**
     * Return mobile NetworkTemplate based on {@code subId}
     */
    public static NetworkTemplate getMobileTemplate(Context context, int subId) {
        final TelephonyManager telephonyManager = context.getSystemService(TelephonyManager.class);
        final int mobileDefaultSubId = telephonyManager.getSubscriptionId();

        final SubscriptionManager subscriptionManager =
                context.getSystemService(SubscriptionManager.class);
        final List<SubscriptionInfo> subInfoList =
                subscriptionManager.getAvailableSubscriptionInfoList();
        if (subInfoList == null) {
            Log.i(TAG, "Subscription is not inited: " + subId);
            return getMobileTemplateForSubId(telephonyManager, mobileDefaultSubId);
        }

        for (SubscriptionInfo subInfo : subInfoList) {
            if ((subInfo != null) && (subInfo.getSubscriptionId() == subId)) {
                return getNormalizedMobileTemplate(telephonyManager, subId);
            }
        }
        Log.i(TAG, "Subscription is not active: " + subId);
        return getMobileTemplateForSubId(telephonyManager, mobileDefaultSubId);
    }

    private static NetworkTemplate getNormalizedMobileTemplate(
            TelephonyManager telephonyManager, int subId) {
        final NetworkTemplate mobileTemplate = getMobileTemplateForSubId(telephonyManager, subId);
        final String[] mergedSubscriberIds = telephonyManager
                .createForSubscriptionId(subId).getMergedImsisFromGroup();
        if (ArrayUtils.isEmpty(mergedSubscriberIds)) {
            Log.i(TAG, "mergedSubscriberIds is null.");
            return mobileTemplate;
        }

        return NetworkTemplate.normalize(mobileTemplate, mergedSubscriberIds);
    }

    public static NetworkTemplate getMobileTemplateForSubId(
            TelephonyManager telephonyManager, int subId) {
        // Create template that matches any mobile network when the subscriberId is null.
        String subscriberId = telephonyManager.getSubscriberId(subId);
        return subscriberId != null
                ? new NetworkTemplate.Builder(NetworkTemplate.MATCH_CARRIER)
                .setSubscriberIds(Set.of(subscriberId))
                .setMeteredness(NetworkStats.METERED_YES)
                .build()
                : new NetworkTemplate.Builder(NetworkTemplate.MATCH_MOBILE)
                        .setMeteredness(NetworkStats.METERED_YES)
                        .build();
    }
}
