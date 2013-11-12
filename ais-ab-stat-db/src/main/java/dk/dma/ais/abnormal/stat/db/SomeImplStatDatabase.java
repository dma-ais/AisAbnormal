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
package dk.dma.ais.abnormal.stat.db;

import java.util.Map;

import dk.dma.enav.model.geometry.grid.Cell;

public class SomeImplStatDatabase implements IStatDatabase {

    @Override
    public Object get(Cell cell) {
        
        return null;
    }

    @Override
    public Object put(Cell cell, Object obj) {
        
        return null;
    }

    @Override
    public Map<Cell, Object> getAll() {
        
        return null;
    }

}
