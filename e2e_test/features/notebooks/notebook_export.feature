Feature: Notebook export for Obsidian
  Background:
    Given I am logged in as an existing user
    And I have a notebook with head note "Taiwan" and notes:
      | Title   | Details    | Parent Title |
      | Taipei  | It is a    | Taiwan       |
      | Weather | It rains a | Taipei       |

  Scenario: Export notebook button can be clicked
    When I go to Notebook page
    And I click on the export for Obsidian option on notebook "Taiwan"

  @ignore
  Scenario: Export notebook as a flat zip file for Obsidian
    When I go to Notebook page
    And I click on the export for Obsidian option on notebook "Taiwan"
    Then I should receive a zip file containing
      | Filename     | Format |
      | Taipei.md    | md     |
      | Weather.md   | md     |
    And the zip file should not contain any subdirectories
    And each markdown file should maintain its original content