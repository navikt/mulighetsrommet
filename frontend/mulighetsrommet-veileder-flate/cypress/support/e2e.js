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
import './e2e';
import 'cypress-axe';

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

if (!app.document.head.querySelector('[data-hide-command-log-request]')) {
  const style = app.document.createElement('style');
  style.innerHTML = '.command-name-request, .command-name-xhr { display: none }';
  style.setAttribute('data-hide-command-log-request', '');

  app.document.head.appendChild(style);
}

before('Start server', () => {
  cy.server();
  cy.visit('/');
  cy.url().should('include', '/');
  Cypress.on('uncaught:exception', err => {
    console.log(err);
    return false;
  });

  cy.getByTestId('tiltakstype-oversikt').children().should('have.length.greaterThan', 1);
});

Cypress.Commands.add('resetSide', () => {
  cy.visit('/');
});

Cypress.Commands.add('getByTestId', (selector, ...args) => {
  return cy.get(`[data-testid=${selector}]`, ...args);
});

Cypress.Commands.add('velgFilter', filternavn => {
  cy.getByTestId(`filter_checkbox_${filternavn}`).click();
  cy.getByTestId(`filter_checkbox_${filternavn}`).should('be.checked');
  cy.getByTestId(`filtertag_${filternavn}`).should('exist');
});

Cypress.Commands.add('fjernFilter', filternavn => {
  cy.getByTestId(`filtertag_lukkeknapp_${filternavn}`).click();
  cy.getByTestId(`filtertag_${filternavn}`).should('not.exist');
});

Cypress.Commands.add('forventetAntallFiltertags', antallForventet => {
  cy.get('.cypress-tag').should('have.length', antallForventet);
});

Cypress.Commands.add('apneLukketFilterAccordion', (filternavn, apne) => {
  if (apne) {
    cy.getByTestId(`filter_accordionheader_${filternavn}`).click();
    cy.getByTestId(`filter_accordioncontent_${filternavn}`).should('exist');
  } else {
    cy.getByTestId(`filter_accordioncontent_${filternavn}`).should('exist');
    cy.getByTestId(`filter_accordionheader_${filternavn}`).click();
  }
});

Cypress.Commands.add('tilbakeTilListevisning', () => {
  cy.getByTestId('tilbakeknapp').contains('Tilbake').click();
});

Cypress.Commands.add('sortering', testId => {
  cy.getByTestId(testId).should('have.attr', 'aria-sort', 'none');

  cy.getByTestId(testId).click();
  cy.getByTestId(testId).should('have.attr', 'aria-sort', 'ascending');

  cy.getByTestId(testId).click();

  cy.getByTestId(testId).should('have.attr', 'aria-sort', 'descending');

  cy.getByTestId(testId).click();

  cy.getByTestId(testId).should('have.attr', 'aria-sort', 'none');
});

//Cypress
const severityIndicators = {
  minor: '⚪️',
  moderate: '🟡',
  serious: '🟠',
  critical: '🔴',
};

function logTerminal(violations) {
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
  cy.configureAxe({
    rules: [
      {
        id: 'svg-img-alt',
        enabled: false,
      },
    ],
  });
  cy.checkA11y({ exclude: [[['.Toastify', '#floating-ui-root', '.navds-tabs__tab-inner']]] }, null, logTerminal);
});
