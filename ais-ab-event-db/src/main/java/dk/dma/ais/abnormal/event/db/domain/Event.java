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

package dk.dma.ais.abnormal.event.db.domain;

import com.google.common.collect.ImmutableSet;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Index;
import org.hibernate.validator.constraints.NotBlank;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * This abstract class is the common class of all Events. An event describes some sort of abnormal behaviour
 * detected among moving vessels in the maritime domain.
 *
 * An Event is also related to at least one vessel. Some event types can be related to further vessels or to
 * other Events.
 */
@Entity
public abstract class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /** State of the event. */
    @NotNull
    @Enumerated(EnumType.STRING)
    private State state = State.ONGOING;

    /** Time on which the event started. */
    @NotNull
    @Index(name="INDEX_EVENT_STARTTIME")
    private Date startTime;

    /** Time on which the event ended. */
    @Index(name="INDEX_EVENT_ENDTIME")
    private Date endTime;

    /** True if this event is suppressed by an operator who concludes that this isn't an event */
    @Index(name="INDEX_EVENT_SUPPRESSED")
    private boolean suppressed = false;

    /** The behaviour observed in connection with this event */
    @OneToMany(cascade = CascadeType.ALL)
    @Fetch(FetchMode.JOIN)
    private Set<Behaviour> behaviours;

    /** A title of the event in English language. */
    @NotBlank
    private String title;

    /** A textual description of the event in English language. */
    private String description;

    protected Event() {
        this.behaviours = new HashSet<>();
    }

    public long getId() {
        return id;
    }

    public String getEventType() {
        return this.getClass().getSimpleName();
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public boolean isSuppressed() {
        return suppressed;
    }

    public void setSuppressed(boolean suppressed) {
        this.suppressed = suppressed;
    }

    public Behaviour getBehaviour(int mmsi) {
        Behaviour behaviour = null;
        try {
            behaviour = behaviours.stream().filter(b -> b.getVessel().getMmsi() == mmsi).findFirst().get();
        } catch (NoSuchElementException e) {
        }
        return behaviour;
    }

    public Set<Behaviour> getBehaviours() {
        return ImmutableSet.copyOf(behaviours);
    }

    public void addBehaviour(Behaviour behaviour) {
        this.behaviours.add(behaviour);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Event{");
        sb.append("id=").append(id);
        sb.append(", state=").append(state);
        sb.append(", startTime=").append(startTime);
        sb.append(", endTime=").append(endTime);
        sb.append(", suppressed=").append(suppressed);
        sb.append(", behaviours=").append(behaviours);
        sb.append(", title='").append(title).append('\'');
        sb.append(", description='").append(description).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public enum State {
        ONGOING,
        PAST;
    };

}
