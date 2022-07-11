describe('Tiltaksgjennomføringstabell', () => {
  let antallTiltak;
  it('Sjekk at det er tiltaksgjennomføringer i tabellen', () => {
    cy.getByTestId('tabell_tiltakstyper').children().children().should('have.length.greaterThan', 1);
  });

  it('Sjekk UU', () => {
    cy.checkPageA11y();
  });

  it('Lagre antall tiltak uten filtrering', () => {
    cy.getByTestId('antall-tiltak').then($navn => {
      antallTiltak = $navn.text();
    });
  });

  it('Filtrer på Innsatsgrupper', () => {
    cy.velgFilter('standardinnsats');

    cy.getByTestId('filtertags').children().should('have.length', 3);
    cy.getByTestId('knapp_tilbakestill-filter').should('exist');

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });

    cy.getByTestId('filtertag_lukkeknapp_standardinnsats').click();
    cy.getByTestId('filtertags').children().should('have.length', 2);
  });

  it('Filtrer på Tiltakstyper', () => {
    cy.apneLukketFilterAccordion('tiltakstyper', true);
    cy.velgFilter('avklaring');
    cy.velgFilter('oppfolging');

    cy.getByTestId('filtertags').children().should('have.length', 4);

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });

    cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();

    cy.getByTestId('filter_checkbox_avklaring').should('not.be.checked');
    cy.getByTestId('filter_checkbox_oppfolging').should('not.be.checked');

    cy.getByTestId('filtertags').children().should('have.length', 2);
    cy.apneLukketFilterAccordion('tiltakstyper', false);
  });

  it('Filtrer på søkefelt', () => {
    cy.getByTestId('filter_sokefelt').type('Digitalt');
    cy.getByTestId('filtertags').children().should('have.length', 3);

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });
    cy.getByTestId('filter_sokefelt').clear();
  });

  it('Sortering', () => {
    //Tester på de forskjellige typene: string, number, date og status
    cy.sortering('tabellheader_tiltaksnavn');
    cy.sortering('tabellheader_tiltaksnummer');
    cy.sortering('tabellheader_oppstartsdato');
    cy.sortering('tabellheader_status');
  });
});

describe('Tiltaksgjennomføringstabell', () => {
  it('Gå til siste tiltaksgjennomføring', () => {
    cy.getByTestId('tabell_tiltaksgjennomforing').last().click();

    cy.wait(1000);
    cy.getByTestId('knapp_kopier').click();
    cy.window().then(win => {
      win.navigator.clipboard.readText().then(text => {
        cy.url().should('include', text);
      });
    });
  });

  it('Sjekk UU', () => {
    cy.checkA11y({ exclude: ['.navds-tooltip'] });
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

  it('Skal ha ferdig utfylt brukers innsatsgruppe', () => {
    // Situasjonsbestemt innsats er innsatsgruppe som returneres når testene kjører med mock-data
    cy.getByTestId('filter_checkbox_situasjonsbestemt-innsats').should('be.checked');
    cy.getByTestId('filtertags').children().should('have.length', 2);
    cy.getByTestId('knapp_tilbakestill-filter').should('not.exist');

    cy.getByTestId('filtertag_situasjonsbestemt-innsats').then($value => {
      expect($value.text()).to.eq('Situasjonsbestemt innsats');
    });
  });
});
