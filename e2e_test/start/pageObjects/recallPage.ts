import { commonSenseSplit } from 'support/string_util'

const recallPage = () => {
  return {
    expectToRecallCounts(numberOfRecalls: string) {
      const [recalledTodayCount, toRecallCountForToday, totalCount] =
        numberOfRecalls.split('/')

      cy.get('.daisy\\:progress-bar').should(
        'contain',
        `Recalling: ${recalledTodayCount}/${toRecallCountForToday}`
      )
      // Click progress bar to show tooltip - use force:true to click even when covered by another element
      cy.get('.daisy\\:progress-bar').first().click({ force: true })

      // Check tooltip content
      cy.get('.tooltip-content').within(() => {
        cy.contains(
          `Daily Progress: ${recalledTodayCount} / ${toRecallCountForToday}`
        )
        cy.contains(`Total assimilated: ${recalledTodayCount} / ${totalCount}`)
      })

      // Close tooltip
      cy.get('.tooltip-popup').click()
    },
    repeatMore() {
      cy.findByRole('button', { name: 'Load more from next 3 days' }).click()
    },
    recallNotes(noteTitles: string) {
      cy.pageIsNotLoading()
      commonSenseSplit(noteTitles, ',').forEach((title) => {
        if (title === 'end') {
          cy.findByText(
            'You have finished all repetitions for this half a day!'
          ).should('be.visible')
        } else {
          cy.findByText(title, { selector: 'h2 *' })
          cy.yesIRemember()
        }
      })
    },
  }
}
export const recall = () => {
  const getRecallListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Recall"]')))

  return {
    expectCount(numberOfNotes: number) {
      getRecallListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.recall-count' })
      })
      return this
    },
    goToRecallPage() {
      cy.routerToRoot()
      getRecallListItemInSidebar(($el) => {
        $el.click()
      })
      return recallPage()
    },
    assumeRecallPage() {
      return recallPage()
    },
  }
}
