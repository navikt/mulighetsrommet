describe('Preview-funksjonalitet for redaktører', () => {
  before(() => {
    Cypress.on('uncaught:exception', err => {
      console.log(err);
      return false;
    });
  });

  beforeEach(() => {
    cy.visit('/preview/11888');
    cy.skruAvJoyride();
    cy.url().should('include', '/preview/');
  });

  it('Skal vise en warning på siden om at man er i Preview-modus', () => {
    cy.getByTestId('sanity-preview-alert').should('exist');
  });

  it('Skal kunne åpne del med bruker, men send via Dialog-knapp gir feilmodal', () => {
    cy.getByTestId('deleknapp').should('exist').click();
    cy.getByTestId('modal_header').should('contain', 'Kunne ikke dele tiltaket');
    cy.getByTestId('modal_btn-cancel').click();
  });
});
