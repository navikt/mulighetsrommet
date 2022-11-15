describe('Preview-funksjonalitet for redaktører', () => {
  before(() => {
    cy.visit('/preview/11888');
  });
  it('Skal vise en warning på siden om at man er i Preview-modus', () => {
    cy.getByTestId('sanity-preview-alert').should('exist');
  });

  it('Skal kunne åpne del med bruker, men send via Dialog-knapp er disabled', () => {
    cy.getByTestId('deleknapp').should('exist').click();
    cy.getByTestId('modal_btn-send').should('be.disabled');
    cy.getByTestId('alert-preview-del-med-bruker').should('exist');
    cy.getByTestId('modal_btn-cancel').click();
  });
});
