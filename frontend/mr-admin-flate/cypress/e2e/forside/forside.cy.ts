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
  context("Header", () => {
    it("Sjekk at navident til admin er i header", () => {
      cy.getByTestId("header-navident").should("exist");
      cy.checkPageA11y();
    });
  });
  context("Landingssiden ved innlogging", () => {
    it("Skal vise kort for tiltaktyper og avtaler på forsiden", () => {
      cy.getByTestId("tiltakstyper").contains("Tiltakstyper");
      cy.getByTestId("avtaler").contains("Avtaler");
      cy.getByTestId("tiltaksgjennomføringer").contains(
        "Tiltaksgjennomføringer",
      );
      cy.checkPageA11y();
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

    it("Skal navigere til tiltaksgjennomføringer og sjekke UU", () => {
      cy.getByTestId("tiltaksgjennomføringer").click();
      cy.url().should("include", "/tiltaksgjennomforinger");
      cy.contains("Oversikt over tiltaksgjennomføringer");
      cy.checkPageA11y();
    });
  });
});
