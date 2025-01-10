import basicActions from './basicActions'
import { higherOrderActions } from './higherOrderActions'
import mock_services from './mock_services/index'
import { questionGenerationService } from './questionGenerationService'
import testability from './testability'
import { assimilation } from './pageObjects/assimilationPage'
import { recall } from './pageObjects/recallPage'
import { assumeCirclePage } from './pageObjects/circlePage'
import { notebookCard } from './pageObjects/notebookCard'

const start = {
  ...basicActions,
  ...higherOrderActions,
  questionGenerationService,
  testability,
  assimilation,
  recall,
  assumeCirclePage,
  notebookCard,
  expectToast(message: string) {
    cy.get('.Vue-Toastification__toast-body', { timeout: 10000 })
      .should('be.visible')
      .and('contain', message)
  },
}
export default start
export { mock_services }
