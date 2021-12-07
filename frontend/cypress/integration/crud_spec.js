before('Start server', () => {
  cy.configure();
});

describe('CRUD tiltaksvariant', () => {
  const tittel = 'Test tiltaksvariant for Cypress';
  const ingress = 'Ingress ingress ingress ingress';
  const beskrivelse =
    'Did you hear about the mathematician who’s afraid of negative numbers? He’ll stop at nothing to avoid them.';

  const nyIngress = 'Laborum officia rerum sed debitis qui odit suscipit aperiam quo.';
  const nyBeskrivelse = 'Laborum officia rerum sed debitis qui odit suscipit aperiam quo.';

  let tabellrader = 0;

  beforeEach('Skal være på listevisninga', () => {
    cy.getByTestId('header-tiltaksvarianter').should('contain', 'Tiltaksvarianter');
    cy.getByTestId('tabell_oversikt-tiltaksvarianter').children().children().should('have.length.at.least', 1);
  });

  afterEach('Fjerner søketekst', () => {
    cy.getByTestId('sokefelt_tiltaksvariant').focus().clear();
  });

  it('Opprett tiltaksvariant', () => {
    const borderWhite = 'rgb(106, 106, 106)';
    const borderRed = 'rgb(186, 58, 38)';
    const feilmeldingTomtFelt = 'Dette feltet kan ikke være tomt';

    cy.getByTestId('tabell_oversikt-tiltaksvarianter')
      .find('.tabell__row')
      .then(listing => {
        tabellrader = Cypress.$(listing).length;
        expect(listing).to.have.length(tabellrader);
      });

    cy.getByTestId('knapp_opprett-tiltaksvariant').click();

    cy.url().should('include', '/tiltaksvarianter/opprett');

    cy.getByTestId('header_opprett-tiltaksvariant').should('contain', 'Opprett tiltaksvariant');

    cy.getByTestId('input_tittel').should('have.css', 'border-color', borderWhite);

    cy.getByTestId('input_ingress').click().should('be.focused').type(ingress);
    cy.getByTestId('input_beskrivelse').click().should('be.focused').type(beskrivelse);

    cy.getByTestId('submit-knapp_opprett-tiltaksvariant').contains('Opprett tiltaksvariant').click();

    cy.getByTestId('form__rediger-opprett').contains(feilmeldingTomtFelt).should('have.css', 'border-color', borderRed);

    cy.getByTestId('input_tittel').click().should('be.focused').type(tittel);

    cy.getByTestId('submit-knapp_opprett-tiltaksvariant').contains('Opprett tiltaksvariant').click();

    cy.get(`.Toastify__toast-container`).should('contain', 'Oppretting vellykket!');

    cy.url().should('include', '/tiltaksvarianter/');

    cy.getByTestId('tiltaksvariant_header').should('contain', tittel);

    cy.getByTestId('tiltaksvariant_ingress').should('contain', ingress);

    cy.getByTestId('tiltaksvariant_beskrivelse').should('contain', beskrivelse);

    cy.tilbakeTilListevisning();

    cy.getByTestId('tabell_oversikt-tiltaksvarianter').contains('td', tittel);
    cy.getByTestId('tabell_oversikt-tiltaksvarianter').contains('td', ingress);

    cy.getByTestId('tabell_oversikt-tiltaksvarianter')
      .find('.tabell__row')
      .then(listing => {
        expect(listing).to.have.length(tabellrader + 1);
      });
  });

  it('Rediger tiltaksvariant', () => {
    cy.getByTestId('sokefelt_tiltaksvariant').focus().type(tittel);

    cy.getByTestId('tabell_oversikt-tiltaksvarianter').children().children().last().contains('td', tittel).click();

    cy.url().should('include', '/tiltaksvarianter/');

    cy.getByTestId('tiltaksvariant_header').should('contain', tittel);
    cy.getByTestId('tiltaksvariant_ingress').should('contain', ingress);
    cy.getByTestId('tiltaksvariant_beskrivelse').should('contain', beskrivelse);

    cy.getByTestId('knapp_rediger-tiltaksvariant').click();

    cy.url().should('include', '/rediger');

    cy.getByTestId('input_ingress').click().should('be.focused').clear().type(nyIngress);
    cy.getByTestId('input_beskrivelse').click().should('be.focused').clear().type(nyBeskrivelse);

    cy.getByTestId('submit-knapp_rediger-tiltaksvariant').click();

    cy.get(`.Toastify__toast-container`).should('contain', 'Endring vellykket');

    cy.url().should('include', '/tiltaksvarianter/');

    cy.tilbakeTilListevisning();
  });

  it('Slett tiltaksvariant', () => {
    cy.getByTestId('tabell_oversikt-tiltaksvarianter')
      .find('.tabell__row')
      .then(listing => {
        expect(listing).to.have.length(tabellrader + 1);
      });

    cy.getByTestId('sokefelt_tiltaksvariant').focus().type(tittel);

    cy.getByTestId('tabell_oversikt-tiltaksvarianter').contains('td', tittel).click();

    cy.getByTestId('tiltaksvariant_header').should('contain', tittel);
    cy.getByTestId('tiltaksvariant_ingress').should('contain', nyIngress);
    cy.getByTestId('tiltaksvariant_beskrivelse').should('contain', nyBeskrivelse);

    cy.getByTestId('knapp_rediger-tiltaksvariant').click();

    cy.getByTestId('slett-knapp_rediger-tiltaksvariant').click();

    cy.getByTestId('rediger-tiltaksvariant__slett-modal__knapperad').click();

    cy.get(`.Toastify__toast-container`).should('contain', 'Sletting vellykket');

    cy.getByTestId('sokefelt_tiltaksvariant').focus().clear();

    cy.getByTestId('tabell_oversikt-tiltaksvarianter')
      .find('.tabell__row')
      .then(listing => {
        expect(listing).to.have.length(tabellrader);
      });
  });

  it('Les tiltaksgjennomføringer', () => {
    const amo = 'Arbeidsmarkedsopplæring (AMO)';
    cy.getByTestId('sokefelt_tiltaksvariant').focus().type(amo);

    cy.getByTestId('tabell_oversikt-tiltaksvarianter').contains('td', amo).click();

    cy.getByTestId('tabell_tiltaksgjennomforinger')
      .children()
      .children()
      .should('have.length.at.least', 1)
      .last()
      .click();

    cy.url().should('include', '/tiltaksgjennomforinger/');

    cy.getByTestId('tilbakeknapp').contains('Tilbake').click();
    cy.getByTestId('tiltaksvariant_header').should('contain', amo);

    cy.url().should('include', '/tiltaksvarianter/');

    cy.tilbakeTilListevisning();
  });
});
