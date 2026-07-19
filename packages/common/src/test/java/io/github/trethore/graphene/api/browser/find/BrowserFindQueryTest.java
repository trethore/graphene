package io.github.trethore.graphene.api.browser.find;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BrowserFindQueryTest {
  @Test
  void preservesSearchTextAndCasePreference() {
    BrowserFindQuery query = new BrowserFindQuery(" Graphene ", true);

    assertEquals(" Graphene ", query.text());
    assertTrue(query.matchCase());
  }

  @Test
  void rejectsNullAndEmptyText() {
    assertThrows(NullPointerException.class, () -> new BrowserFindQuery(null, false));
    assertThrows(IllegalArgumentException.class, () -> new BrowserFindQuery("", false));
  }
}
