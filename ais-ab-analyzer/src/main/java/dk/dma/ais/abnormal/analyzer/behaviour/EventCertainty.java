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

package dk.dma.ais.abnormal.analyzer.behaviour;

/**
 * A measure of the certainty of the state of a given event class.
 */
public enum EventCertainty {
    /**
     * The event certainty is undefined.
     */
    UNDEFINED(0),
    /**
     * An event of a given eventClass is lowered and the most recent track updates assert this - i.e. the most
     * recent track updates confirmed that the event should be and remain low/lowered.
     */
    LOWERED(1),
    /**
     * Events of a given eventClass is lowered but the most recent track updates indicate, that it should be
     * raised (but still isn't). Or the opposite - that eventClass is raised and the most recent track updates
     * indicate that it should be lowered (but still isn't).
     */
    UNCERTAIN(2),
    /**
     * An event of a given eventClass is raised and the most recent track updates assert this - i.e. the most
     * recent track updates confirmed that the event should be and remain raised.
     */
    RAISED(3);

    public static EventCertainty create(int certainty) {
        return EventCertainty.values()[certainty];
    }

    private int certainty;

    public int getCertainty() {
        return certainty;
    }

    EventCertainty(int certainty) {
        this.certainty = certainty;
    }
}
