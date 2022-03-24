before('Start server', () => {
  cy.configure();
});

describe('Mulighetsrommet', () => {
  //TODO fiks denne når ny frontend er klar
  it('Sjekk at det er tiltak i listen', () => {
    cy.checkPageA11y();
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
    cy.getByTestId('tabell_tiltakstyper_tiltaksnummer').first().click();

    cy.getByTestId('main-view-header_opplaering').should('be.visible');
    cy.checkPageA11y();
    cy.getByTestId('btn_send-informasjon').click();

    cy.get('.ReactModal__Content').should('be.visible');
    cy.getByTestId('modal_header').contains('Informasjon om Opplæring');
    cy.getByTestId('textarea_send-informasjon').type('Dette er informasjon om tiltakstypen opplæring.');
    cy.getByTestId('modal_btn-cancel').contains('Avbryt').click(); //TODO send denne istedenfor å avbryte når Grafana er oppe og går
    cy.get('.ReactModal__Content').should('not.exist');

    cy.getByTestId('btn_gi-tilbakemelding').click();

    cy.get('.ReactModal__Content').should('be.visible');
    cy.getByTestId('modal_header').contains('Tilbakemelding');
    cy.getByTestId('textarea_tilbakemelding').type('Her kommer en kjempefin tilbakemelding trudeluuu.');
    cy.getByTestId('modal_btn-cancel').contains('Avbryt').click(); //TODO send denne istedenfor å avbryte når Grafana er oppe og går
    cy.get('.ReactModal__Content').should('not.exist');
  });
});
