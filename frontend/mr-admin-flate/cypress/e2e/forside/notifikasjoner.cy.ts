before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    // eslint-disable-next-line no-console
    console.log(err);
    return false;
  });
});

describe("Notifikasjoner", () => {
  context("Navigering til notifikasjoner", () => {
    it("Skal navigere fra forside til side for notifikasjoner via notifikasjonsbjelle", () => {
      cy.visit("/");
      cy.getByTestId("notifikasjonsbjelle").should("exist").click();
      cy.getByTestId("notifikasjon-tag")
        .should("exist")
        .contains("Notifikasjon");
      cy.getByTestId("oppgave-tag").should("exist").contains("Oppgave");
      cy.checkPageA11y();
    });
  });

  it("Skal navigere til ressurs hvis det er lenket til ressurs i notifikasjonstittel eller beskrivelse", () => {
    cy.visit("/notifikasjoner");
    cy.getByTestId("notifikasjon")
      .eq(0)
      .get("[data-testid=notifikasjon] a")
      .should("have.attr", "href")
      .then((href) => {
        cy.visit(String(href));
        cy.contains("Avtale hos ÅMLI KOMMUNE SAMFUNNSAVDELINGA");
      });
    cy.checkPageA11y();
  });
});
