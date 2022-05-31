describe('Tiltaksgjennomføring', () => {
  it('Sjekk at det er tiltaksgjennomføringer i listen', () => {
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 17);
    cy.checkPageA11y();
  });

  it('Gå inn på den første tiltaksgjennomføringen', () => {
    cy.getByTestId('tabell_tiltakstyper_tiltaksnummer').first().click();
    cy.url().should('include', '/tiltakstyper/ABIST');
    cy.getByTestId('tiltaksgjennomforing-header_opplaering').should('be.visible');
    cy.checkPageA11y();
  });

  it('Gå tilbake til tiltaksoversikten', () => {
    cy.tilbakeTilListevisning();
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
  });
});

describe('Filtrering', () => {
  it('Sjekk at det er tiltaksgjennomføringer i listen, og at tilbakestill filter-knapp ikke er tilgjengelig', () => {
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
  });

  it('Filtrer på Innsatsgrupper', () => {
    cy.getByTestId('knapp_tilbakestill-filter').should('not.exist');

    cy.velgFilter('standardinnsats');

    cy.getByTestId('filtertags').children().should('have.length', 1);

    cy.getByTestId('knapp_tilbakestill-filter').should('exist');
  });

  it('Filtrer på Tiltakstyper', () => {
    cy.apneLukketFilterAccordion('tiltakstyper', true);

    cy.velgFilter('arbeidsforberedende-trening');
    cy.velgFilter('arbeidstrening');

    cy.getByTestId('filtertags').children().should('have.length', 3);
  });

  it('Tilbakestill filtre', () => {
    cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();

    cy.getByTestId('filter_checkbox_standardinnsats').should('not.be.checked');
    cy.getByTestId('filter_checkbox_arbeidsforberedende-trening').should('not.be.checked');
    cy.getByTestId('filter_checkbox_arbeidstrening').should('not.be.checked');

    cy.getByTestId('filtertags').children().should('have.length', 0);

    cy.apneLukketFilterAccordion('tiltakstyper', false);
  });
});

describe('Kopiknapp og sortering', () => {
  it('Kopiknapp', () => {
    const tiltaksnummer = cy
      .getByTestId('tiltaksnummer')
      .first()
      .then($text => {
        return $text.text();
      });

    cy.getByTestId('knapp_kopier').first().click();

    cy.window().then(win => {
      win.navigator.clipboard.readText().then(text => {
        expect(text).to.eq(tiltaksnummer);
      });
    });
  });
  it('Sortering', () => {
    cy.getByTestId('tabellheader_tiltaksnavn').should('have.attr', 'aria-sort', 'none');

    cy.getByTestId('tabellheader_tiltaksnavn').click();
    cy.getByTestId('tabellheader_tiltaksnavn').should('have.attr', 'aria-sort', 'ascending');

    cy.getByTestId('tabellheader_tiltaksnavn').click();
    cy.getByTestId('tabellheader_tiltaksnavn').should('have.attr', 'aria-sort', 'descending');

    cy.getByTestId('tabellheader_tiltaksnavn').click();
  });
});
