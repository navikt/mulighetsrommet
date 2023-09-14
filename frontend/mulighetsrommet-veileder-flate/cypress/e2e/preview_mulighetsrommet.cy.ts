describe('Preview-funksjonalitet for redaktører', () => {
  before(() => {
    Cypress.on('uncaught:exception', err => {
      // eslint-disable-next-line no-console
      console.log(err);
      return false;
    });
  });

  beforeEach(() => {
    cy.visit('/preview/f4cea25b-c372-4d4c-8106-535ab10cd586');
    cy.skruAvJoyride();
    cy.url().should('include', '/preview/');
  });

  it('Skal vise en warning på siden om at man er i Preview-modus', () => {
    cy.getByTestId('sanity-preview-alert').should('exist');
  });

  it('Skal kunne åpne del med bruker, men send via Dialog-knapp gir feilmodal', () => {
    cy.getByTestId('deleknapp').should('exist').click();
    cy.getByTestId('alert-preview-del-med-bruker').should(
      'contain',
      'Det er ikke mulig å dele tiltak med bruker i forhåndsvisning'
    );
    cy.getByTestId('modal_btn-cancel').click();
  });
});
