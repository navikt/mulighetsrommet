before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});

describe("Tiltaksgjennomføringer", () => {
  context("Navigering til tiltakstypedetaljer", () => {
    it("Skal kunne klikke på rad for tiltakstype og navigere til detaljer", () => {
      cy.visit("/tiltakstyper");
      cy.getByTestId("tiltakstyperad").eq(0).click();
      cy.getByTestId("tab_arenainfo").should("exist");
      cy.getByTestId("tab_avtaler").should("exist");
      cy.checkPageA11y();
    });
  });
});
