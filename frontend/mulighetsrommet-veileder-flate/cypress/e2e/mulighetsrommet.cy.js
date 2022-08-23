describe('Tiltaksgjennomføringstabell', () => {
  let antallTiltak;
  it('Sjekk at det er tiltaksgjennomføringer i tabellen', () => {
    cy.getByTestId('tabell_tiltaksgjennomforing').should('have.length.greaterThan', 1);
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

    cy.getByTestId('filtertags').children().should('have.length', 4);
    cy.getByTestId('knapp_tilbakestill-filter').should('exist');

    cy.wait(1000);
    cy.getByTestId('antall-tiltak').then($navn => {
      expect(antallTiltak).not.to.eq($navn.text());
    });

    cy.getByTestId('filtertag_lukkeknapp_standardinnsats').click();
    cy.getByTestId('filtertags').children().should('have.length', 2);
  });

  it('Filtrer på Tiltakstyper', () => {
    cy.apneLukketFilterAccordion('tiltakstyper', true);
    cy.velgFilter('avklaring');
    cy.velgFilter('oppfolging');

    cy.getByTestId('filtertags').children().should('have.length', 5);

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
    cy.fjernFilter('situasjonsbestemt-innsats');
    cy.getByTestId('filter_sokefelt').type('AFT');
    cy.getByTestId('filtertags').children().should('have.length', 3);

    cy.wait(1000)
      .getByTestId('antall-tiltak')
      .then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });
    cy.getByTestId('filter_sokefelt').clear();
    cy.getByTestId('filtertags').children().should('have.length', 2);
  });

  it('Skal vise tilbakestill filter-knapp når filter utenfor normalen', () => {
    cy.velgFilter('situasjonsbestemt-innsats');
    cy.getByTestId('knapp_tilbakestill-filter').should('not.exist');
    cy.fjernFilter('situasjonsbestemt-innsats');
    cy.getByTestId('knapp_tilbakestill-filter').should('exist');
  });

  it('Skal kunne navigere mellom sider via paginering', () => {
    cy.getByTestId('paginering').should('exist');
    cy.getByTestId('paginering').children().children().eq(1).should('not.have.attr', 'aria-current');
    cy.getByTestId('paginering').children().children().eq(2).click();
    cy.getByTestId('paginering').children().children().children().eq(2).should('have.attr', 'aria-current');
  });
});

xdescribe('Tiltaksgjennomføringsdetaljer', () => {
  it('Gå til en tiltaksgjennomføring', () => {
    cy.getByTestId('tabell_tiltaksgjennomforing').first().click();
  });

  it('Sjekk at tiltaksnummer tilsvarer med url', () => {
    cy.getByTestId('knapp_kopier').click();

    cy.window().then(win => {
      win.navigator.clipboard.readText().then(text => {
        cy.url().should('include', text);
      });
    });
  });

  it('Sjekk UU', () => {
    cy.checkPageA11y();
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

  it('Skal vise korrekt feilmelding dersom ingen tiltaksgjennomføringer blir funnet', () => {
    cy.getByTestId('filter_sokefelt').type('blablablablabla');
    cy.getByTestId('feilmelding-container').should('be.visible');
    cy.getByTestId('feilmelding-container').should('have.attr', 'aria-live');
  });
});
