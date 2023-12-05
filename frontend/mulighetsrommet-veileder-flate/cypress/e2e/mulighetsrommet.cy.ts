before("Start server", () => {
  cy.visit("/arbeidsmarkedstiltak/oversikt");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    // eslint-disable-next-line no-console
    console.log(err);
    return false;
  });

  cy.getByTestId("tiltakstype-oversikt").children().should("have.length.greaterThan", 1);
});

describe("Tiltaksoversikt", () => {
  let antallTiltak: number;

  beforeEach(() => {
    cy.clearLocalStorage();
    cy.visit("/arbeidsmarkedstiltak/oversikt");
  });

  it("Sjekk at det er tiltaksgjennomføringer i oversikten", () => {
    cy.getByTestId("oversikt_tiltaksgjennomforinger")
      .children()
      .should("have.length.greaterThan", 1);
  });

  it("Sjekk UU", () => {
    cy.checkPageA11y();
  });

  it("Lagre antall tiltak uten filtrering", () => {
    cy.getByTestId("antall-tiltak").then(($navn) => {
      antallTiltak = Number.parseInt($navn.text());
    });
  });

  it("Filtrer på Tiltakstyper", () => {
    cy.apneLukketFilterAccordion("tiltakstyper", true);
    cy.velgFilter("mentor");

    cy.getByTestId("antall-tiltak").then(($navn) => {
      expect(antallTiltak).not.to.eq($navn.text());
    });

    cy.getByTestId("knapp_tilbakestill-filter").should("exist").click();

    cy.getByTestId("filter_checkbox_avklaring").should("not.be.checked");
    cy.getByTestId("filter_checkbox_mentor").should("not.be.checked");

    cy.apneLukketFilterAccordion("tiltakstyper", false);
  });

  it("Filtrer på søkefelt", () => {
    cy.getByTestId("filter_sokefelt").type("Yoda", { delay: 250 });
    cy.getByTestId("lenke_tiltaksgjennomforing").contains("Yoda");
  });

  it("Skal vise tilbakestill filter-knapp når filter utenfor normalen hvis brukeren har innsatsgruppe", () => {
    cy.velgFilter("standard-innsats");
    cy.getByTestId("knapp_tilbakestill-filter").should("exist");
  });

  it("Skal legge løpende tiltaksgjennomføringer først i rekken ved sortering på oppstartsdato", () => {
    cy.getByTestId("sortering-select").select("oppstart-ascending");
    cy.getByTestId("lenke_tiltaksgjennomforing").eq(0).contains("Løpende oppstart");
  });

  it("Skal kunne navigere mellom sider via paginering", () => {
    cy.getByTestId("paginering").should("exist");
    cy.getByTestId("paginering")
      .children()
      .children()
      .eq(1)
      .should("not.have.attr", "aria-current");
    cy.getByTestId("paginering").children().children().eq(2).click();
    cy.getByTestId("paginering")
      .children()
      .children()
      .children()
      .eq(2)
      .should("have.attr", "aria-current");
  });

  it("Skal ha ferdig utfylt brukers innsatsgruppe hvis bruker har innsatsgruppe", () => {
    cy.resetSide();
    // Situasjonsbestemt innsats er innsatsgruppen som returneres når testene kjører med mock-data
    cy.getByTestId("filter_checkbox_situasjonsbestemt-innsats").should("be.checked");
    cy.getByTestId("knapp_tilbakestill-filter").should("not.exist");

    cy.getByTestId("filtertag_situasjonsbestemt-innsats").then(($value) => {
      expect($value.text()).to.eq("Situasjonsbestemt innsats");
    });
  });

  it("Skal huske filtervalg mellom detaljvisning og listevisning", () => {
    cy.getByTestId("filter_checkbox_standard-innsats").click();
    cy.forventetAntallFiltertags(2);
    cy.getByTestId("filter_checkbox_situasjonsbestemt-innsats").click();

    cy.getByTestId("lenke_tiltaksgjennomforing").first().click();
    cy.tilbakeTilListevisning();
    cy.getByTestId("filter_checkbox_situasjonsbestemt-innsats").should("be.checked");
    cy.forventetAntallFiltertags(2);
  });

  it("Skal vise korrekt feilmelding dersom ingen tiltaksgjennomføringer blir funnet", () => {
    cy.getByTestId("filter_sokefelt").type("blablablablabla", { delay: 250 });
    cy.getByTestId("feilmelding-container").should("be.visible");
    cy.getByTestId("feilmelding-container").should("have.attr", "aria-live");
    cy.getByTestId("knapp_tilbakestill-filter").should("exist").click();
  });
});

describe("Tiltaksgjennomføringsdetaljer", () => {
  beforeEach(() => {
    cy.clearLocalStorage();
    cy.visit("/arbeidsmarkedstiltak/oversikt");
    cy.getByTestId("lenke_tiltaksgjennomforing").first().click();
  });

  it("Gå til en tiltaksgjennomføring", () => {
    cy.checkPageA11y();
  });

  it("Sjekk at fanene fungerer som de skal", () => {
    cy.getByTestId("tab1").should("be.visible");
    cy.getByTestId("tab2").should("not.be.visible");

    cy.getByTestId("fane_detaljer-og-innhold").click();

    cy.getByTestId("tab1").should("not.be.visible");
    cy.getByTestId("tab2").should("be.visible");
  });

  it("Sjekk 'Del med bruker'", () => {
    cy.getByTestId("deleknapp").should("be.visible").click();

    cy.getByTestId("modal_header").should("be.visible");
    cy.getByTestId("personlig_intro_btn").click();
    cy.getByTestId("textarea_intro").type("En spennende tekst", { delay: 250 });
    cy.get(".navds-error-message").should("not.exist");

    cy.getByTestId("personlig_hilsen_btn").click();

    cy.getByTestId("textarea_hilsen").type("Test", { delay: 250 });
    cy.get(".navds-error-message").should("not.exist");

    cy.getByTestId("modal_btn-send").should("not.be.disabled").click();

    cy.getByTestId("modal_header").should("contain", "Tiltaket er delt med brukeren");
    cy.getByTestId("modal_btn-cancel").last().click();
  });
});
