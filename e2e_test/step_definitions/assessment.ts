import { Then, When } from "@badeball/cypress-cucumber-preprocessor"
import "../support/string_util"
import start from "../start"

When("I start the assessment on the {string} notebook in the bazaar", (notebook: string) => {
  start.navigateToBazaar().selfAssessmentOnNotebook(notebook)
})

Then("I answer the question {string} with {string}", function (stem: string, answer: string) {
  start.assumeAssessmentPage().expectQuestion(stem).answer(answer)
})

Then("I should see end of assessment in the end", () => {
  start.assumeAssessmentPage().expectEndOfAssessment()
})

Then("I see error message Not enough approved questions", () => {
  cy.findByText("Not enough approved questions").should("be.visible")
})
