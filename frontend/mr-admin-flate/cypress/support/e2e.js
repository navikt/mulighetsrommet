// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import e2e.js using ES2015 syntax:
import "./e2e";
import "cypress-axe";

// Alternatively you can use CommonJS syntax:
// require('cypress-dark');

// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
// -- This is a parent command --
// Cypress.Commands.add('login', (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add('drag', { prevSubject: 'element'}, (subject, options) => { ... })
//
// -- This is a dual command --
// Cypress.Commands.add('dismiss', { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite('visit', (originalFn, url, options) => { ... })

// https://github.com/cypress-io/cypress/issues/7362#issuecomment-944273204
// Hide fetch/XHR requests
const app = window.top;

if (!app.document.head.querySelector("[data-hide-command-log-request]")) {
  const style = app.document.createElement("style");
  style.innerHTML =
    ".command-name-request, .command-name-xhr { display: none }";
  style.setAttribute("data-hide-command-log-request", "");

  app.document.head.appendChild(style);
}

Cypress.Commands.add("resetSide", () => {
  cy.visit("/");
});

Cypress.Commands.add("getByTestId", (selector, ...args) => {
  return cy.get(`[data-testid=${selector}]`, ...args);
});

function terminalLog(violations) {
  cy.task(
    "log",
    `${violations.length} accessibility violation${
      violations.length === 1 ? "" : "s"
    } ${violations.length === 1 ? "was" : "were"} detected`
  );
  // pluck specific keys to keep the table readable
  const violationData = violations.map(
    ({ id, impact, description, nodes }) => ({
      id,
      impact,
      description,
      nodes: nodes.length,
    })
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
    ],
  });
  cy.checkA11y(
    {
      exclude: [[[".Toastify", "#floating-ui-root", ".navds-tabs__tab-inner"]]],
    },
    null,
    terminalLog
  );
});
