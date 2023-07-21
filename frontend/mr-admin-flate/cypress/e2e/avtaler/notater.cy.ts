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
    let antallNotater: number;
    let antallNotaterEtterLagring: number;
    let antallMineNotater: number;
    let antallNotaterEtterSletting: number;

    it("Skal kunne lage et nytt og slette notater", () => {
      cy.gaTilForsteAvtale();
      cy.getByTestId("tab_avtalenotater").click();
      cy.get(".navds-error-message").should("not.exist");

      cy.getByTestId("notatliste")
        .find('[data-testid="notat"]')
        .then(($value) => {
          antallNotater = $value.length;
        });

      cy.getByTestId("notater_innhold").type("Dette er");
      cy.getByTestId("notater_legg-til-knapp").click();
      cy.get(".navds-error-message").should("be.visible");
      cy.getByTestId("notater_innhold").type(" et kjempefint notat");
      cy.getByTestId("notater_legg-til-knapp").click();

      cy.getByTestId("notatliste")
        .find('[data-testid="notat"]')
        .then(($value) => {
          antallNotaterEtterLagring = $value.length;
        });

      cy.getByTestId("notatliste").then(() => {
        expect(antallNotater).lessThan(antallNotaterEtterLagring);
      });
      cy.getByTestId("vis-mine-notater").click();
      cy.getByTestId("notat_brukerinformasjon")
        .find('[data-testid="slette-notat_btn"]')
        .then(($value) => {
          antallMineNotater = $value.length;
        });

      cy.getByTestId("notat_brukerinformasjon").then(() => {
        expect(antallNotater).greaterThan(antallMineNotater);
      });

      cy.getByTestId("vis-mine-notater").click();

      cy.getByTestId("slette-notat_btn").first().click();
      cy.getByTestId("bekrefte-slette-notat_btn").click();

      cy.getByTestId("notatliste")
        .find('[data-testid="notat"]')
        .then(($value) => {
          antallNotaterEtterSletting = $value.length;
        });

      cy.getByTestId("notatliste").then(() => {
        expect(antallNotater).equals(antallNotaterEtterSletting);
      });
    });
  });
});
