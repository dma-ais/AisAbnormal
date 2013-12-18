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

package dk.dma.ais.abnormal.event.db.domain.builders;

import dk.dma.ais.abnormal.event.db.domain.Vessel;

public class VesselBuilder {

    Vessel vessel;
    AbnormalShipSizeOrTypeEventBuilder abnormalShipSizeOrTypeEventBuilder;

    public VesselBuilder() {
        vessel = new Vessel();
    }

    public VesselBuilder(AbnormalShipSizeOrTypeEventBuilder abnormalShipSizeOrTypeEventBuilder) {
        vessel = new Vessel();
        this.abnormalShipSizeOrTypeEventBuilder = abnormalShipSizeOrTypeEventBuilder;
    }

    public static VesselBuilder Vessel(){
        return new VesselBuilder();
    }

    public VesselBuilder name(String name){
        vessel.setName(name);
        return this;
    }

    public VesselBuilder callsign(String callsign){
        vessel.setCallsign(callsign);
        return this;
    }

    public VesselBuilder imo(int imo){
        vessel.setImo(imo);
        return this;
    }

    public VesselBuilder mmsi(int mmsi){
        vessel.setMmsi(mmsi);
        return this;
    }

    public BehaviourBuilder behaviour(){
        BehaviourBuilder builder = new BehaviourBuilder(this);
        getVessel().setBehaviour(builder.getBehaviour());
        return builder;
    }

    public Vessel getVessel() {
        return vessel;
    }

}
