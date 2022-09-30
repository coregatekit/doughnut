package com.odde.doughnut.services.externalApis;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import java.util.Optional;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WikidataEntityModel {
  private Map<String, WikidataEntityItemModel> entities;

  private WikidataEntityItemModel getEntityItem(String wikidataId) {
    return entities.get(wikidataId);
  }

  private Optional<WikidataValue> getFirstClaimOfProperty(String wikidataId, String propertyId) {
    return getEntityItem(wikidataId).getFirstClaimOfProperty(propertyId);
  }

  public Optional<String> getDescription(String wikidataId) {
    if (getWikiClass(wikidataId).equals(Optional.of("Q5"))) {
      // P569: Birthday
      return getFirstClaimOfProperty(wikidataId, "P569").map(WikidataValue::toDateDescription);
    } else {
      return getFirstClaimOfProperty(wikidataId, "P625").map(WikidataValue::toLocationDescription);
    }
  }

  public Optional<String> getLocationPhotoFileName(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, "P18").map(WikidataValue::getStringValue);
  }

  private Optional<String> getWikiClass(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, "P31").map(WikidataValue::toWikiClass);
  }

  public Optional<String> getLocationMapFileName(String wikidataId) {
    return getFirstClaimOfProperty(wikidataId, "P1943").map(WikidataValue::getStringValue);
  }
}
