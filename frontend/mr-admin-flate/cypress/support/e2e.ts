import axe from "axe-core";
import "cypress-axe";

declare global {
  namespace Cypress {
    interface Chainable {
      resetSide(): Chainable<JQuery<HTMLElement>>;
      getByTestId(
        selector: string,
        ...rest: any
      ): Chainable<JQuery<HTMLElement>>;
      terminalLog(vialations: axe.Result[]): Chainable<JQuery<HTMLElement>>;
      checkPageA11y(): Chainable<JQuery<HTMLElement>>;
      gaTilForsteAvtale(): Chainable<JQuery<HTMLElement>>;
    }
  }
}

const app = window.top;

if (
  app &&
  !app?.document.head.querySelector("[data-hide-command-log-request]")
) {
  const style = app.document.createElement("style");
  style.innerHTML =
    ".command-name-request, .command-name-xhr { display: none }";
  style.setAttribute("data-hide-command-log-request", "");

  app?.document.head.appendChild(style);
}

Cypress.Commands.add("resetSide", () => {
  cy.visit("/");
});

Cypress.Commands.add("getByTestId", (selector, ...args) => {
  return cy.get(`[data-testid=${selector}]`, ...args);
});

Cypress.Commands.add("gaTilForsteAvtale", () => {
  cy.visit("/avtaler");
  cy.getByTestId("avtalerad").eq(0).click();
});

function terminalLog(violations: axe.Result[]) {
  cy.task(
    "log",
    `${violations?.length} accessibility violation${
      violations?.length === 1 ? "" : "s"
    } ${violations?.length === 1 ? "was" : "were"} detected`,
  );
  // pluck specific keys to keep the table readable
  const violationData = violations.map(
    ({ id, impact, description, nodes }) => ({
      id,
      impact,
      description,
      nodes: nodes.length,
    }),
  );

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
