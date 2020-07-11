/*
 * WiFiAnalyzer
 * Copyright (C) 2015 - 2020 VREM Software Development <VREMSoftwareDevelopment@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.vrem.wifianalyzer.wifi.predicate

import com.vrem.wifianalyzer.settings.Settings
import com.vrem.wifianalyzer.wifi.band.WiFiBand
import com.vrem.wifianalyzer.wifi.model.Security
import com.vrem.wifianalyzer.wifi.model.Strength
import com.vrem.wifianalyzer.wifi.model.WiFiDetail

interface Predicate {
    fun test(wiFiDetail: WiFiDetail): Boolean
}

internal class TruePredicate : Predicate {
    override fun test(wiFiDetail: WiFiDetail): Boolean = true
}

internal class FalsePredicate : Predicate {
    override fun test(wiFiDetail: WiFiDetail): Boolean = false
}

internal class AnyPredicate(private val predicates: List<Predicate>) : Predicate {
    override fun test(wiFiDetail: WiFiDetail): Boolean = predicates.any { it.test(wiFiDetail) }
}

internal class AllPredicate(private val predicates: List<Predicate>) : Predicate {
    override fun test(wiFiDetail: WiFiDetail): Boolean = predicates.all { it.test(wiFiDetail) }
}

typealias ToPredicate<T> = (T) -> Predicate

private val wiFiBand: ToPredicate<WiFiBand> = { WiFiBandPredicate(it) }
private val strength: ToPredicate<Strength> = { StrengthPredicate(it) }
private val security: ToPredicate<Security> = { SecurityPredicate(it) }

internal fun <T : Enum<T>> predicate(values: Array<T>, filter: Set<T>, toPredicate: ToPredicate<T>): Predicate =
        if (filter.size >= values.size)
            TruePredicate()
        else
            AnyPredicate(filter.map { toPredicate(it) })

private fun makeSSIDPredicate(ssids: Set<String>): Predicate =
        if (ssids.isEmpty())
            TruePredicate()
        else
            AnyPredicate(ssids.map { SSIDPredicate(it) })

private fun makePredicate(settings: Settings, wiFiBands: Set<WiFiBand>): AllPredicate =
        AllPredicate(sequenceOf(
                makeSSIDPredicate(settings.findSSIDs()),
                predicate(WiFiBand.values(), wiFiBands, wiFiBand),
                predicate(Strength.values(), settings.findStrengths(), strength),
                predicate(Security.values(), settings.findSecurities(), security))
                .toList())

fun makeAccessPointsPredicate(settings: Settings): Predicate = makePredicate(settings, settings.findWiFiBands())

fun makeOtherPredicate(settings: Settings): Predicate = makePredicate(settings, setOf(settings.wiFiBand()))
