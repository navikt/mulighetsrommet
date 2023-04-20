before('Start server', () => {
  cy.visit('/');
  cy.url().should('include', '/');
  Cypress.on('uncaught:exception', err => {
    console.log(err);
    return false;
  });

  cy.getByTestId('tiltakstype-oversikt').children().should('have.length.greaterThan', 1);
});

describe('Tiltaksoversikt', () => {
  let antallTiltak;
  let kvalifiseringsgruppe;
  const servicegruppe = 'service';
  const innsatsgruppe = 'innsats';

  beforeEach(() => {
    cy.visit('/');
    cy.skruAvJoyride();
    cy.resetSortering();
  });

  it('Sjekk at det er tiltaksgjennomføringer i oversikten', () => {
    cy.getByTestId('oversikt_tiltaksgjennomforinger').children().should('have.length.greaterThan', 1);
  });

  it('Sjekk UU', () => {
    cy.checkPageA11y();
  });

  it('Sjekk om bruker har innsatsgruppe eller servicegruppe', () => {
    const path = 'nokkel';

    cy.url().then($url => {
      if ($url.includes(path)) {
        kvalifiseringsgruppe = innsatsgruppe;
      } else {
        kvalifiseringsgruppe = servicegruppe;
      }
    });
  });

  it('Sjekk at varsel vises hvis brukeren har servicegruppe', () => {
    if (kvalifiseringsgruppe === servicegruppe) {
      cy.getByTestId('varsel_servicesgruppe').should('be.visible');
    }
  });

  context('Filtrering', () => {
    it('Lagre antall tiltak uten filtrering', () => {
      cy.getByTestId('antall-tiltak').then($navn => {
        antallTiltak = $navn.text();
      });
    });

    it('Filtrer på Innsatsgrupper hvis brukeren har innsatsgruppe', () => {
      if (kvalifiseringsgruppe === innsatsgruppe) {
        cy.velgFilter('standardinnsats');

        cy.forventetAntallFiltertags(2);
        cy.getByTestId('knapp_tilbakestill-filter').should('exist');

        cy.getByTestId('antall-tiltak').then($navn => {
          expect(antallTiltak).not.to.eq($navn.text());
        });

        cy.forventetAntallFiltertags(2);
        cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();
      }
    });

    it('Filtrer på Tiltakstyper', () => {
      cy.apneLukketFilterAccordion('tiltakstyper', true);
      cy.velgFilter('avklaring');
      cy.velgFilter('oppfolging');

      cy.antallFiltertagsKvalifiseringsgruppe(kvalifiseringsgruppe, 3);

      cy.getByTestId('antall-tiltak').then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });

      cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();

      cy.getByTestId('filter_checkbox_avklaring').should('not.be.checked');
      cy.getByTestId('filter_checkbox_oppfolging').should('not.be.checked');

      cy.antallFiltertagsKvalifiseringsgruppe(kvalifiseringsgruppe, 1);
      cy.apneLukketFilterAccordion('tiltakstyper', false);
    });

    it('Filtrer på lokasjoner', () => {
      cy.apneLukketFilterAccordion('lokasjon', true);
      cy.getByTestId('checkboxgroup_lokasjon').children().children().first().click();

      cy.antallFiltertagsKvalifiseringsgruppe(kvalifiseringsgruppe, 2);

      cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();

      cy.getByTestId('checkboxgroup_lokasjon').children().children().should('not.be.checked');
      cy.apneLukketFilterAccordion('lokasjon', false);
    });

    it('Filtrer på søkefelt', () => {
      if (kvalifiseringsgruppe === innsatsgruppe) {
        cy.velgFilter('varig-tilpasset-innsats');
      }
      cy.getByTestId('filter_sokefelt').type('AFT');
      cy.forventetAntallFiltertags(3);

      cy.getByTestId('antall-tiltak').then($navn => {
        expect(antallTiltak).not.to.eq($navn.text());
      });

      cy.getByTestId('filter_sokefelt').clear();
      cy.forventetAntallFiltertags(2);
    });

    it('Skal vise tilbakestill filter-knapp når filter utenfor normalen hvis brukeren har innsatsgruppe', () => {
      if (kvalifiseringsgruppe === innsatsgruppe) {
        cy.velgFilter('standardinnsats');
        cy.getByTestId('knapp_tilbakestill-filter').should('exist');
      }
    });
  });

  context('Sortering', () => {
    it('Skal legge løpende tiltaksgjennomføringer først i rekken ved sortering på oppstartsdato', () => {
      cy.getByTestId('sortering-select').select('oppstart-ascending');
      cy.getByTestId('lenke_tiltaksgjennomforing').eq(0).contains('Løpende oppstart');
    });
  });

  it('Skal kunne navigere mellom sider via paginering', () => {
    cy.getByTestId('paginering').should('exist');
    cy.getByTestId('paginering').children().children().eq(1).should('not.have.attr', 'aria-current');
    cy.getByTestId('paginering').children().children().eq(2).click();
    cy.getByTestId('paginering').children().children().children().eq(2).should('have.attr', 'aria-current');
  });

  it('Skal ha ferdig utfylt brukers innsatsgruppe hvis bruker har innsatsgruppe', () => {
    if (kvalifiseringsgruppe === innsatsgruppe) {
      // Situasjonsbestemt innsats er innsatsgruppen som returneres når testene kjører med mock-data
      cy.resetSide();
      cy.getByTestId('filter_checkbox_situasjonsbestemt-innsats').should('be.checked');
      cy.antallFiltertagsKvalifiseringsgruppe(kvalifiseringsgruppe, 1);
      cy.getByTestId('knapp_tilbakestill-filter').should('not.exist');

      cy.getByTestId('filtertag_situasjonsbestemt-innsats').then($value => {
        expect($value.text()).to.eq('Situasjonsbestemt innsats');
      });
    }
  });

  it('Skal huske filtervalg mellom detaljvisning og listevisning', () => {
    cy.getByTestId('filter_checkbox_standardinnsats').click();
    cy.forventetAntallFiltertags(2);

    cy.getByTestId('lenke_tiltaksgjennomforing').first().click();
    cy.tilbakeTilListevisning();
    cy.getByTestId('filter_checkbox_standardinnsats').should('be.checked');
    cy.forventetAntallFiltertags(2);
  });

  it('Skal vise korrekt feilmelding dersom ingen tiltaksgjennomføringer blir funnet', () => {
    cy.getByTestId('filter_sokefelt').type('blablablablabla');
    cy.getByTestId('feilmelding-container').should('be.visible');
    cy.getByTestId('feilmelding-container').should('have.attr', 'aria-live');
    cy.getByTestId('knapp_tilbakestill-filter').should('exist').click();
  });
});

describe('Tiltaksgjennomføringsdetaljer', () => {
  beforeEach(() => {
    cy.visit('/');
    cy.skruAvJoyride();
    cy.getByTestId('lenke_tiltaksgjennomforing').first().click();
  });

  it('Gå til en tiltaksgjennomføring', () => {
    cy.checkPageA11y();
  });

  it('Sjekk at tilgjengelighetsstatus er tilgjengelig på detaljsiden', () => {
    cy.getByTestId('tilgjengelighetsstatus_detaljside').should('exist');
  });

  it('Sjekk at fanene fungerer som de skal', () => {
    cy.getByTestId('tab1').should('be.visible');
    cy.getByTestId('tab2').should('not.be.visible');

    cy.getByTestId('fane_detaljer-og-innhold').click();

    cy.getByTestId('tab1').should('not.be.visible');
    cy.getByTestId('tab2').should('be.visible');
  });

  it("Sjekk 'Del med bruker'", () => {
    cy.getByTestId('deleknapp').should('be.visible').click();

    cy.getByTestId('modal_header').should('be.visible');
    cy.getByTestId('personlig_hilsen_btn').click();

    cy.getByTestId('textarea_hilsen').type('Test');
    cy.get('.navds-error-message').should('not.exist');

    cy.getByTestId('modal_btn-send').should('not.be.disabled').click();

    cy.getByTestId('modal_header').should('contain', 'Tiltaket er delt med brukeren');
    cy.getByTestId('modal_btn-cancel').click();
    cy.getByTestId('modal_header').should('not.exist');
  });
});
