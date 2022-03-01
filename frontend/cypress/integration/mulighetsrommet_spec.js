before('Start server', () => {
  cy.configure();
});

describe('Mulighetsrommet', () => {
  //TODO fiks denne når ny frontend er klar
  it('Check page a11y', () => {
    cy.checkPageA11y();
  });

  // it('Testytest', () => {
  //   cy.getByTestId('tabell_oversikt-tiltakstyper').find('tr').should('have.length.greaterThan', 1);
  // });
});
