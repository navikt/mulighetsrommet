before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});

describe("Forside", () => {
  it("Sjekk at navident til admin er i header", () => {
    cy.getByTestId("header-navident").should("exist");
    cy.checkPageA11y();
  });
});

describe("Gjennomføringer", () => {
  it("Skal kunne navigere til ansatt sin liste med tiltaksgjennomføringer", () => {
    cy.getByTestId("tab-mine").click();
    cy.url("include", "/mine");
    cy.getByTestId("tiltaksgjennomforingsrad").eq(0).click();
    cy.wait(500);
    cy.getByTestId("fjern-favoritt").should("exist");
    cy.getByTestId("tilbakelenke").click();
  });
});
