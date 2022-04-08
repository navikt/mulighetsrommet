describe('Mulighetsrommet', () => {
  it('Sjekk at det er tiltaksgjennomføringer i listen', () => {
    cy.checkPageA11y();
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
  });

  it('Gå inn på den første tiltaksgjennomføringen', () => {
    cy.getByTestId('tabell_tiltakstyper_rad').first().click();
    cy.url().should('include', '/tiltakstyper/ABIST');
    cy.getByTestId('main-view-header_opplaering').should('be.visible');
    cy.checkPageA11y();
  });

  it('Klikk på gi tilbakemeldingsknappen', () => {
    cy.getByTestId('btn_gi-tilbakemelding').click();
    cy.checkPageA11y();
    cy.get('.ReactModal__Content').should('be.visible');
    cy.getByTestId('modal_header').contains('Tilbakemelding');
    cy.getByTestId('textarea_tilbakemelding').type('Her kommer en kjempefin tilbakemelding trudeluuu.');
    cy.getByTestId('modal_btn-send').contains('Send').click();
    cy.get('.ReactModal__Content').should('not.exist');
    cy.get('.Toastify__toast').should('be.visible').and('contain', 'Takk for din tilbakemelding!');
  });

  it('Gå tilbake til tiltaksoversikten', () => {
    cy.tilbakeTilListevisning();
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
  });
});
