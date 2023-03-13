before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});

describe("Forside", () => {
  beforeEach(() => {
    cy.visit("/");
  });
  context("Landingssiden ved innlogging", () => {
    it("Skal vise kort for tiltaktyper og avtaler på forsiden", () => {
      cy.getByTestId("tiltakstyper").contains("Tiltakstyper");
      cy.getByTestId("avtaler").contains("Avtaler");
    });

    it("Skal navigere til tiltakstyper og sjekke UU", () => {
      cy.getByTestId("tiltakstyper").click();
      cy.url().should("include", "/tiltakstyper");
      cy.contains("Oversikt over tiltakstyper");
      cy.checkPageA11y();
    });

    it("Skal navigere til avtaler og sjekke UU", () => {
      cy.getByTestId("avtaler").click();
      cy.url().should("include", "/avtaler");
      cy.contains("Oversikt over avtaler");
      cy.checkPageA11y();
    });
  });
});

describe("Tiltakstyper", () => {
  beforeEach(() => {
    cy.visit("/tiltakstyper");
  });

  context("Header", () => {
    it("Sjekk at navident til admin er i header", () => {
      cy.getByTestId("header-navident").should("exist");
      cy.checkPageA11y();
    });
  });

  context("Filtrering for tiltakstyper", () => {
    it("Bruker skal ha et søkefelt for tiltakstyper", () => {
      cy.getByTestId("filter_sokefelt").should("exist");
    });

    it("Bruker skal ha et valg for å filtrere på statuser", () => {
      cy.getByTestId("filter_status").should("exist");
      cy.getByTestId("filter_status").select("Aktiv").should("exist");
      cy.getByTestId("filter_status").select("Planlagt").should("exist");
      cy.getByTestId("filter_status").select("Avsluttet").should("exist");
    });

    it("Bruker skal ha et valg for å filtrere på gruppe- eller individuelle tiltak", () => {
      cy.getByTestId("filter_kategori").should("exist");
      cy.getByTestId("filter_kategori").select("GRUPPE").should("exist");
      cy.getByTestId("filter_kategori").select("INDIVIDUELL").should("exist");
    });
  });
});

describe("Detaljside for tiltakstyper", () => {
  context("Navigering til tiltakstypedetaljer", () => {
    it("Skal kunne klikke på rad for tiltakstype og navigere til detaljer", () => {
      cy.visit("/tiltakstyper");
      cy.getByTestId("tiltakstyperad").eq(0).click();
      cy.getByTestId("tab_arenainfo").should("exist");
      cy.getByTestId("tab_avtaler").should("exist");
      cy.getByTestId("tab_nokkeltall").should("exist");
      cy.checkPageA11y();
    });
  });
});

describe("Detaljside for avtale", () => {
  context("Navigering til avtaledetaljer", () => {
    it("Skal kunne klikke på en avtale og navigere til avtaledetaljer", () => {
      cy.visit("/tiltakstyper");
      cy.getByTestId("tiltakstyperad").eq(0).click();
      cy.contains("Avtaler");
      cy.getByTestId("tab_avtaler").click();
      cy.getByTestId("avtalerad").eq(0).click();
      cy.checkPageA11y();
    });
  });
});
