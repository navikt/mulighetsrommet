before("Start server", () => {
  cy.visit("/");
  cy.url().should("include", "/");
  Cypress.on("uncaught:exception", (err) => {
    console.log(err);
    return false;
  });
});

describe("Utkast", () => {
  context("Tab for utkast for gjennomføringer knyttet til en avtale", () => {
    it("Skal finnes en tab for 'Mine utkast'", () => {
      cy.gaTilForsteAvtale();
      cy.getByTestId("avtale-tiltaksgjennomforing-tab").click();
      cy.getByTestId("mine-utkast-tab").should("exist").click();
      cy.getByTestId("rediger-utkast-knapp").should("exist");
      cy.getByTestId("slett-utkast-knapp").should("exist");
      cy.checkPageA11y();
    });

    it.skip("Skal kunne opprette et utkast og se det i oversikten over utkast", () => {
      cy.gaTilForsteAvtale();
      cy.getByTestId("avtale-tiltaksgjennomforing-tab").click();
      cy.getByTestId("opprett-gjennomforing-knapp").click();
      cy.getByTestId("tiltaksgjennomforingnavn-input").type("Tester data");
      cy.wait(1100); // Simuler en bruker som bruker over 1 sek på å skrive
      //TODO må endres når vi legger til lagre-knapp
      cy.getByTestId("avbryt-knapp").click();
      cy.getByTestId("mine-utkast-tab").should("exist").click();
      cy.wait(150);
      //TODO fikser når vi legger til lagre-knapp
      // cy.contains("Tester data");
    });
  });

  context("Tab for utkast for avtaler", () => {
    it("Skal finnes en tab for 'Mine utkast'", () => {
      cy.visit("/avtaler");
      cy.getByTestId("mine-utkast-tab").should("exist").click();
      cy.getByTestId("rediger-utkast-knapp").should("exist");
      cy.getByTestId("slett-utkast-knapp").should("exist");
      cy.checkPageA11y();
    });

    it("Skal kunne opprette et utkast og se det i oversikten over utkast", () => {
      cy.visit("/avtaler");
      cy.getByTestId("avtaler-tab").click();
      cy.getByTestId("opprett-avtale").click();
      cy.getByTestId("avtalenavn-input").type("Avtale som utkast");
      cy.wait(1100); // Simuler en bruker som bruker over 1 sek på å skrive
      cy.getByTestId("avtaleskjema-avbrytknapp").click();
      cy.wait(150);
      cy.getByTestId("mine-utkast-tab").should("exist").click();
      cy.contains("Avtale som utkast");
    });
  });
});
