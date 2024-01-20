package com.odde.doughnut.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "thing")
public class Thing extends EntityIdentifiedByIdOnly {

  @Column(name = "created_at")
  @Setter
  @Getter
  private Timestamp createdAt;

  @Column(name = "deleted_at")
  @JsonIgnore
  @Setter
  private Timestamp deletedAt;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "note_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  private Note note;

  @OneToOne(cascade = CascadeType.PERSIST)
  @JoinColumn(name = "link_id", referencedColumnName = "id")
  @Getter
  @Setter
  @Nullable
  private Link link;

  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creator;

  static <T extends Thingy> T createThing(User user, T thingy, Timestamp currentUTCTimestamp) {
    final Thing thing = new Thing();
    if (thingy instanceof Note note) thing.setNote(note);
    if (thingy instanceof Link link) thing.setLink(link);
    thing.setCreator(user);
    thing.setCreatedAt(currentUTCTimestamp);
    thingy.setThing(thing);
    return thingy;
  }

  @JsonIgnore
  Note getHeadNoteOfNotebook() {
    Note result;
    if (getLink() != null) {
      result = getLink().getSourceNote();
    } else {
      result = getNote();
    }

    return result.getNotebook().getHeadNote();
  }

  @JsonIgnore
  public boolean isDescriptionBlankHtml() {
    return getNote().isDetailsBlankHtml();
  }
}
