describe('Preview-funksjonalitet for redaktører', () => {
  before(() => {
    Cypress.on('uncaught:exception', err => {
      console.log(err);
      return false;
    });
  });

  beforeEach(() => {
    cy.visit('/preview/11888');
    cy.url().should('include', '/preview/');
  });

  it('Skal vise en warning på siden om at man er i Preview-modus', () => {
    cy.getByTestId('sanity-preview-alert').should('exist');
  });

  it('Skal kunne åpne del med bruker, men send via Dialog-knapp er disabled', () => {
    cy.getByTestId('deleknapp').should('exist').click();
    cy.getByTestId('delemodal-alert').should('be.visible');
    cy.get('.navds-modal__button').click();
  });
});
