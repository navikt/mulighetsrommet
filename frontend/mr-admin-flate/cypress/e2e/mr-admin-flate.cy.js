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
