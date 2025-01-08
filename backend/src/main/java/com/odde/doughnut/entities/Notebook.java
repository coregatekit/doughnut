package com.odde.doughnut.entities;

import static com.odde.doughnut.controllers.dto.ApiError.ErrorType.ASSESSMENT_SERVICE_ERROR;
import static com.theokanning.openai.service.OpenAiService.defaultObjectMapper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.models.Randomizer;
import com.odde.doughnut.services.graphRAG.BareNote;
import jakarta.persistence.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.NonNull;

@Entity
@Table(name = "notebook")
@JsonPropertyOrder({
  "id",
  "certifiable",
  "notebookSettings",
  "creatorId",
  "title",
  "circle",
  "headNoteId",
  "shortDetails"
})
public class Notebook extends EntityIdentifiedByIdOnly {
  @OneToOne
  @JoinColumn(name = "creator_id")
  @JsonIgnore
  @Getter
  @Setter
  private User creatorEntity;

  @OneToOne
  @JoinColumn(name = "ownership_id")
  @Getter
  @Setter
  @JsonIgnore
  private Ownership ownership;

  @JoinTable(
      name = "notebook_head_note",
      joinColumns = {@JoinColumn(name = "notebook_id", referencedColumnName = "id")},
      inverseJoinColumns = {@JoinColumn(name = "head_note_id", referencedColumnName = "id")})
  @OneToOne
  @Getter
  @Setter
  @NonNull
  @JsonIgnore
  private Note headNote;

  @OneToMany(mappedBy = "notebook", cascade = CascadeType.DETACH)
  @JsonIgnore
  private final List<Note> notes = new ArrayList<>();

  @Embedded @Getter @NonNull private NotebookSettings notebookSettings = new NotebookSettings();

  @Column(name = "deleted_at")
  @Setter
  @JsonIgnore
  private Timestamp deletedAt;

  @OneToMany(mappedBy = "notebook", cascade = CascadeType.DETACH)
  @JsonIgnore
  private List<Subscription> subscriptions;

  @OneToOne(mappedBy = "notebook")
  @Getter
  @JsonIgnore
  private NotebookCertificateApproval notebookCertificateApproval;

  @Column(name = "updated_at")
  @Getter
  @Setter
  @NonNull
  private Timestamp updated_at;

  @OneToOne(mappedBy = "notebook", fetch = FetchType.LAZY)
  @JsonIgnore
  @Getter
  private NotebookAssistant notebookAssistant;

  @OneToOne(mappedBy = "notebook", fetch = FetchType.LAZY)
  @JsonIgnore
  @Getter
  private NotebookAiAssistant notebookAiAssistant;

  public boolean isCertifiable() {
    return notebookCertificateApproval != null
        && notebookCertificateApproval.getLastApprovalTime() != null;
  }

  @JsonIgnore
  public List<Note> getNotes() {
    return Note.filterDeletedUnmodifiableNoteList(notes);
  }

  // Hibernate and JPA does not maintain the consistency of the bidirectional relationships
  // Here we add the note to the notes of notebook in memory to avoid reload the notebook from
  // database
  public void addNoteInMemoryToSupportUnitTestOnly(Note note) {
    this.notes.add(note);
  }

  @JsonIgnore
  public String getNotebookDump() {
    List<BareNote> noteBriefs = getNoteBriefs();
    return defaultObjectMapper().valueToTree(noteBriefs).toPrettyString();
  }

  @JsonIgnore
  public List<BareNote> getNoteBriefs() {
    List<BareNote> noteBriefs =
        notes.stream()
            .sorted(
                Comparator.comparing(Note::getParentId, Comparator.nullsFirst(Integer::compare))
                    .thenComparing(Note::getSiblingOrder, Comparator.nullsFirst(Long::compare)))
            .map(n -> BareNote.fromNoteWithoutTruncate(n))
            .toList();
    ;
    return noteBriefs;
  }

  public String getCreatorId() {
    return creatorEntity.getExternalIdentifier();
  }

  @JsonIgnore
  public List<PredefinedQuestion> getApprovedPredefinedQuestionsForAssessment(
      Randomizer randomizer) {
    Integer numberOfQuestion = getNotebookSettings().getNumberOfQuestionsInAssessment();
    if (numberOfQuestion == null || numberOfQuestion == 0) {
      throw new ApiException(
          "The assessment is not available",
          ASSESSMENT_SERVICE_ERROR,
          "The assessment is not available");
    }

    List<PredefinedQuestion> questions =
        randomizer.shuffle(getNotes()).stream()
            .flatMap(
                note ->
                    randomizer
                        .chooseOneRandomly(
                            note.getPredefinedQuestions().stream()
                                .filter(PredefinedQuestion::isApproved)
                                .toList())
                        .stream())
            .limit(numberOfQuestion)
            .toList();

    if (questions.size() < numberOfQuestion) {
      throw new ApiException(
          "Not enough questions", ASSESSMENT_SERVICE_ERROR, "Not enough questions");
    }
    return questions;
  }

  @NonNull
  public String getTitle() {
    return headNote.getTopicConstructor();
  }

  @NonNull
  public Integer getHeadNoteId() {
    return headNote.getId();
  }

  public String getShortDetails() {
    return headNote.getShortDetails();
  }

  public Circle getCircle() {
    return getOwnership().getCircle();
  }

  @JsonIgnore
  public NotebookAssistant buildOrEditNotebookAssistant(
      Timestamp currentUTCTimestamp, User creator, String id) {
    NotebookAssistant notebookAssistant = getNotebookAssistant();
    if (notebookAssistant == null) {
      notebookAssistant = new NotebookAssistant();
      notebookAssistant.setNotebook(this);
    }
    notebookAssistant.setCreator(creator);
    notebookAssistant.setCreatedAt(currentUTCTimestamp);
    notebookAssistant.setAssistantId(id);
    return notebookAssistant;
  }

  @JsonIgnore
  public byte[] generateObsidianExport() throws IOException {

    try (var baos = new ByteArrayOutputStream();
        var zos = new ZipOutputStream(baos)) {

      writeNoteToZip(this.getHeadNote(), zos, "");

      zos.close();
      return baos.toByteArray();
    }
  }

  private void writeNoteToZip(Note note, ZipOutputStream zos, String path) throws IOException {
    // 檢查是否有子筆記
    boolean hasChildren = !note.getChildren().isEmpty();

    // �建檔案路徑
    String filePath;
    if (hasChildren) {
      // 如果有子筆記，使用 __index.md
      filePath =
          path.isEmpty()
              ? note.getTopicConstructor() + "/__index.md"
              : path + "/" + note.getTopicConstructor() + "/__index.md";
    } else {
      // 如果沒有子筆記，使用原來的命名方式
      filePath =
          path.isEmpty()
              ? note.getTopicConstructor() + ".md"
              : path + "/" + note.getTopicConstructor() + ".md";
    }

    // 建立檔案內容
    String fileContent = "# " + note.getTopicConstructor() + "\n" + note.getDetails();
    zos.putNextEntry(new ZipEntry(filePath));
    zos.write(fileContent.getBytes());

    // 遞迴處理子筆記
    for (Note child : note.getChildren()) {
      writeNoteToZip(
          child,
          zos,
          path.isEmpty() ? note.getTopicConstructor() : path + "/" + note.getTopicConstructor());
    }
  }

  private String sanitizeFileName(String fileName) {
    return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
  }
}
