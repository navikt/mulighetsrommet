before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});

describe("Notater", () => {
  context("Notater på avtaler", () => {
    const tekst =
      "Alle barna kastet snøball på læreren unntatt Svein, han kastet stein.";

    it("Skal kunne lage et nytt og slette notater", () => {
      cy.gaTilForsteAvtale();
      cy.getByTestId("tab_avtalenotater").click();
      cy.get(".navds-error-message").should("not.exist");

      cy.getByTestId("notatliste").should("not.contain", tekst);

      cy.getByTestId("notater_innhold").type("Epic fail");
      cy.getByTestId("notater_legg-til-knapp").click();
      cy.get(".navds-error-message").should("be.visible");
      cy.getByTestId("notater_innhold").clear();
      cy.getByTestId("notater_innhold").type(tekst);
      cy.getByTestId("notater_legg-til-knapp").click();

      cy.getByTestId("notatliste").contains(tekst);
      cy.getByTestId("vis-mine-notater").click();

      cy.getByTestId("slette-notat_btn").first().click();
      cy.getByTestId("bekrefte-slette-notat_btn").click();
      cy.getByTestId("notatliste").should("not.contain", tekst);
    });
  });
});
