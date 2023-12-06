import "./e2e";
import "cypress-axe";
import axe from "axe-core";
import "cypress-localstorage-commands";

declare global {
  namespace Cypress {
    interface Chainable {
      resetSide(): Chainable<JQuery<HTMLElement>>;
      getByTestId(selector: string, ...rest: any): Chainable<JQuery<HTMLElement>>;
      terminalLog(vialations: axe.Result[]): Chainable<JQuery<HTMLElement>>;
      checkPageA11y(): Chainable<JQuery<HTMLElement>>;
      velgFilter(filternavn: string): Chainable<JQuery<HTMLElement>>;
      fjernFilter(filternavn: string): Chainable<JQuery<HTMLElement>>;
      forventetAntallFiltertags(forventetAntall: number): Chainable<JQuery<HTMLElement>>;
      apneLukketFilterAccordion(filternavn: string, apne: boolean): Chainable<JQuery<HTMLElement>>;
      tilbakeTilListevisning(): Chainable<JQuery<HTMLElement>>;
      sortering(testid: string): Chainable<JQuery<HTMLElement>>;
      resetSortering(): Chainable<JQuery<HTMLElement>>;
      antallFiltertagsKvalifiseringsgruppe(
        kvalifiseringsgruppe: string,
        antallFilter: number,
      ): Chainable<JQuery<HTMLElement>>;
    }
  }
}

const app = window.top;

if (app && !app.document.head.querySelector("[data-hide-command-log-request]")) {
  const style = app.document.createElement("style");
  style.innerHTML = ".command-name-request, .command-name-xhr { display: none }";
  style.setAttribute("data-hide-command-log-request", "");

  app.document.head.appendChild(style);
}

Cypress.Commands.add("resetSide", () => {
  cy.visit("/arbeidsmarkedstiltak/oversikt");
});

Cypress.Commands.add("getByTestId", (selector, ...args) => {
  return cy.get(`[data-testid=${selector}]`, ...args);
});

Cypress.Commands.add("velgFilter", (filternavn) => {
  cy.getByTestId(`filter_checkbox_${filternavn}`).should("exist").click();
  cy.getByTestId(`filter_checkbox_${filternavn}`).should("be.checked");
  cy.getByTestId(`filtertag_${filternavn}`).should("exist");
});

Cypress.Commands.add("fjernFilter", (filternavn) => {
  cy.getByTestId(`filtertag_lukkeknapp_${filternavn}`).click();
  cy.getByTestId(`filtertag_${filternavn}`).should("not.exist");
});

Cypress.Commands.add("forventetAntallFiltertags", (antallForventet) => {
  cy.get(".cypress-tag").should("have.length", antallForventet);
});

Cypress.Commands.add("apneLukketFilterAccordion", (filternavn, apne) => {
  if (apne) {
    cy.getByTestId(`filter_accordionheader_${filternavn}`).click();
    cy.getByTestId(`filter_accordioncontent_${filternavn}`).should("exist");
  } else {
    cy.getByTestId(`filter_accordioncontent_${filternavn}`).should("exist");
    cy.getByTestId(`filter_accordionheader_${filternavn}`).click();
  }
});

Cypress.Commands.add("tilbakeTilListevisning", () => {
  cy.getByTestId("tilbakeknapp").contains("Tilbake").click();
});

Cypress.Commands.add("sortering", (testId) => {
  cy.getByTestId(testId).should("have.attr", "aria-sort", "none");

  cy.getByTestId(testId).click();
  cy.getByTestId(testId).should("have.attr", "aria-sort", "ascending");

  cy.getByTestId(testId).click();

  cy.getByTestId(testId).should("have.attr", "aria-sort", "descending");

  cy.getByTestId(testId).click();

  cy.getByTestId(testId).should("have.attr", "aria-sort", "none");
});

Cypress.Commands.add(
  "antallFiltertagsKvalifiseringsgruppe",
  (kvalifiseringsgruppe, antallFilter) => {
    if (kvalifiseringsgruppe === "service") {
      cy.forventetAntallFiltertags(antallFilter);
    } else {
      cy.forventetAntallFiltertags(antallFilter + 1);
    }
  },
);

Cypress.Commands.add("resetSortering", () => {
  cy.getByTestId("sortering-select").select("tiltakstypeNavn-ascending");
});

function terminalLog(violations: axe.Result[]) {
  cy.task(
    "log",
    `${violations?.length} accessibility violation${violations?.length === 1 ? "" : "s"} ${
      violations?.length === 1 ? "was" : "were"
    } detected`,
  );
  // pluck specific keys to keep the table readable
  const violationData = violations.map(({ id, impact, description, nodes }) => ({
    id,
    impact,
    description,
    nodes: nodes.length,
  }));

  cy.task("table", violationData);
}

Cypress.Commands.add("checkPageA11y", () => {
  cy.injectAxe();
  cy.configureAxe({
    rules: [
      {
        id: "svg-img-alt",
        enabled: false,
      },
      //Skrur av fordi checkA11y ikke vet at div er en gyldig children av <dl>-elementer
      {
        id: "dlitem",
        enabled: false,
      },
    ],
  });
  cy.checkA11y(
    {
      exclude: [[[".Toastify", "#floating-ui-root", ".navds-tabs__tab-inner"]]],
    },
    undefined,
    terminalLog,
  );
});
