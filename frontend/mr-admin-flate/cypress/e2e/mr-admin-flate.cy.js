before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});
describe("Forside", () => {
  it("Sjekk at det er en overskrift på forsiden", () => {
    cy.getByTestId("admin-heading").should("exist");
  });

  it("Sjekk UU", () => {
    cy.checkPageA11y();
  });
});
