before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});

describe("Avtaler", () => {
  context("Navigering til avtaledetaljer", () => {
    it("Skal kunne klikke på en avtale og navigere til avtaledetaljer", () => {
      cy.visit("/tiltakstyper");
      cy.getByTestId("filter_sokefelt").type("Arbeidsrettet");
      cy.getByTestId("tiltakstyperad").eq(0).click();
      cy.contains("Avtaler");
      cy.getByTestId("tab_avtaler").click();
      cy.gaTilForsteAvtale();
      cy.checkPageA11y();
    });

    it("Skal ha mulighet til å endre en avtale", () => {
      cy.gaTilForsteAvtale();
      cy.checkPageA11y();
      cy.getByTestId("endre-avtale").should("exist").click();
      cy.getByTestId("rediger-avtale-header").contains("Rediger avtale");
    });
  });

  context("Oversikt over avtaler", () => {
    it("Skal kunne opprette en ny avtale", () => {
      cy.visit("/avtaler");
      cy.getByTestId("avtaler-tab").click();
      cy.getByTestId("opprett-avtale").should("exist").click();
      cy.getByTestId("opprett-avtale-header").contains("Opprett ny avtale");
    });
  });
});
