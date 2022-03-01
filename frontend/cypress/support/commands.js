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

Cypress.Commands.add('configure', () => {
  cy.server();
  cy.visit('/');
  cy.url().should('include', '/');
  Cypress.on('uncaught:exception', err => {
    console.log(err);
    return false;
  });
  cy.route({
    method: 'GET',
    url: '/',
  });
  cy.getByTestId('tiltakstype-oversikt').children().should('have.length.greaterThan', 1);
});

Cypress.Commands.add('getByTestId', (selector, ...args) => {
  return cy.get(`[data-testid=${selector}]`, ...args);
});

Cypress.Commands.add('tilbakeTilListevisning', () => {
  cy.getByTestId('tilbakeknapp').contains('Tilbake').click();
  cy.getByTestId('header-tiltakstyper').should('contain', 'Tiltakstyper');
});

//Cypress
const severityIndicators = {
  minor: '⚪️',
  moderate: '🟡',
  serious: '🟠',
  critical: '🔴',
};

function callback(violations) {
  violations.forEach(violation => {
    const nodes = Cypress.$(violation.nodes.map(node => node.target).join(','));

    Cypress.log({
      name: `${severityIndicators[violation.impact]} A11Y`,
      consoleProps: () => violation,
      $el: nodes,
      message: `[${violation.help}](${violation.helpUrl}`,
    });

    violation.nodes.forEach(({ target }) => {
      Cypress.log({
        name: '🔧',
        consoleProps: () => violation,
        $el: Cypress.$(target.join(',')),
        message: target,
      });
    });
  });
}

Cypress.Commands.add('checkPageA11y', () => {
  cy.injectAxe();
  cy.checkA11y({ exclude: [[['.Toastify']]] }, null, callback);
});
