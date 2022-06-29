describe('Tiltaksgjennomføringstabell', () => {
  let antallTiltak;
  it('Sjekk at det er tiltaksgjennomføringer i tabellen', () => {
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
    // cy.checkPageA11y();
  });

  it('Lagre antall tiltak uten filtrering', () => {
    cy.getByTestId('antall-tiltak').then($navn => {
      antallTiltak = $navn.text();
    });
  });

  it('Filtrer på Innsatsgrupper', () => {
    cy.getByTestId('knapp_tilbakestill-filter').should('not.exist');

    cy.velgFilter('standardinnsats');

    cy.getByTestId('filtertags').children().should('have.length', 1);
    cy.getByTestId('knapp_tilbakestill-filter').should('exist');

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });

    cy.getByTestId('filtertag_lukkeknapp_standardinnsats').click();
    cy.getByTestId('filtertags').children().should('have.length', 0);
  });

  it('Filtrer på Tiltakstyper', () => {
    cy.apneLukketFilterAccordion('tiltakstyper', true);
    cy.velgFilter('avklaring');
    cy.velgFilter('oppfolging');

    cy.getByTestId('filtertags').children().should('have.length', 2);

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });

    cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();

    cy.getByTestId('filter_checkbox_avklaring').should('not.be.checked');
    cy.getByTestId('filter_checkbox_oppfolging').should('not.be.checked');

    cy.getByTestId('filtertags').children().should('have.length', 0);
    cy.apneLukketFilterAccordion('tiltakstyper', false);
  });

  it('Filtrer på søkefelt', () => {
    cy.getByTestId('filter_sokefelt').type('Digitalt');
    cy.getByTestId('filtertags').children().should('have.length', 1);

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });
  });

  it('Sortering', () => {
    //Tester på de forskjellige typene: string, number, date og status
    cy.sortering('tabellheader_tiltaksnavn');
    cy.sortering('tabellheader_tiltaksnummer');
    cy.sortering('tabellheader_oppstartsdato');
    cy.sortering('tabellheader_status');
  });

  it('Kopiknapp', () => {
    const tiltaksnummer = cy
      .getByTestId('tabell_tiltaksnummer')
      .last()
      .then($text => {
        return $text.text();
      });
    cy.getByTestId('tabell_knapp_kopier').last().click();

    cy.window().then(win => {
      win.navigator.clipboard.readText().then(text => {
        expect(text).to.eq(tiltaksnummer);
      });
    });
  });

  it('Gå til siste tiltaksgjennomføring', () => {
    cy.getByTestId('tabell_tiltaksgjennomforing').last().click();

    cy.getByTestId('knapp_kopier').click();
    cy.window().then(win => {
      win.navigator.clipboard.readText().then(text => {
        cy.url().should('include', text);
      });
    });
  });
});

describe('Tiltaksgjennomføringstabell', () => {
  it('Sjekk UU', () => {
    // cy.checkA11y({ exclude: ['.navds-tooltip'] });
  });

  it('Sjekk at fanene fungerer som de skal', () => {
    cy.getByTestId('tab1').should('be.visible');
    cy.getByTestId('tab2').should('not.be.visible');

    cy.getByTestId('fane_detaljer-og-innhold').click();

    cy.getByTestId('tab1').should('not.be.visible');
    cy.getByTestId('tab2').should('be.visible');
  });

  it('Gå tilbake til tiltaksoversikten', () => {
    cy.tilbakeTilListevisning();
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
  });
});
