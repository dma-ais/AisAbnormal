/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dma.ais.abnormal.util;

import dk.dma.ais.abnormal.tracker.Track;

import java.util.function.Predicate;

/**
 * The TrackPredicates class is here to assist in uniform business rules across the application.
 * It determines what is considered a 'slow vessel', and 'small vessel', a vessel
 * 'engaged in <something>' and so on using function predicates.
 *
 * @author Thomas Borg Salling <tbsalling@tbsalling.dk>
 */
public final class TrackPredicates {

    private TrackPredicates() {
    }

    public static Predicate<Track> isSpecialCraft = track -> {
        Integer shipType = track.getShipType();
        return shipType != null && shipType >= 50 && shipType <= 55;
    };

    public static Predicate<Track> isTankerVessel = track -> shipTypeCategoryEquals(track, 1);

    public static Predicate<Track> isCargoVessel = track -> shipTypeCategoryEquals(track, 2);

    public static Predicate<Track> isPassengerVessel = track -> shipTypeCategoryEquals(track, 3);

    public static Predicate<Track> isSupportVessel = track -> shipTypeCategoryEquals(track, 4);

    public static Predicate<Track> isFishingVessel = track -> shipTypeCategoryEquals(track, 5);

    public static Predicate<Track> isClassB = track -> shipTypeCategoryEquals(track, 6);

    public static Predicate<Track> isUndefinedVessel = track -> shipTypeCategoryEquals(track, 8);

    public static Predicate<Track> isUnknownTypeOrSize = track -> track.getShipType() == null || track.getVesselLength() == null;

    public static Predicate<Track> isSlowVessel = track -> track.getSpeedOverGround() < 3.0;

    public static Predicate<Track> isLongVessel = track -> {
        Integer length = track.getVesselLength();
        return length == null ? false : length.intValue() >= 30;
    };

    public static Predicate<Track> isVeryLongVessel = track -> {
        Integer length = track.getVesselLength();
        return length == null ? false : length.intValue() >= 75;
    };

    public static Predicate<Track> isSmallVessel = track -> {
        Integer length = track.getVesselLength();
        return length == null ? false : length < 30;
    };

    public static Predicate<Track> isEngagedInTowing = track -> {
        Integer shipType = track.getShipType();
        return shipType == null ? false : shipType == 31 || shipType == 32;
    };

    public static Predicate<Track> isEngagedInFishing = track -> {
        Integer shipType = track.getShipType();
        return shipType == null ? false : shipType == 30;
    };

    private static boolean shipTypeCategoryEquals(Track track, int category) {
        Integer shipType = track.getShipType();
        return shipType != null && Categorizer.mapShipTypeToCategory(shipType) == category;
    }

}
