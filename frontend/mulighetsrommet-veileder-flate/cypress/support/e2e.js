before('Start server and inject Axe', () => {
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

Cypress.Commands.add('velgFilter', filternavn => {
  cy.getByTestId(`filter_checkbox_${filternavn}`).should('not.be.checked').click().should('be.checked');
  cy.getByTestId(`filtertag_${filternavn}`).should('exist');
});

Cypress.Commands.add('sortering', dataTestId => {
  cy.getByTestId(dataTestId).should('have.attr', 'aria-sort', 'none');
  cy.getByTestId(dataTestId).click();
  cy.getByTestId(dataTestId).should('have.attr', 'aria-sort', 'ascending');
  cy.getByTestId(dataTestId).click();
  cy.getByTestId(dataTestId).should('have.attr', 'aria-sort', 'descending');
  cy.getByTestId(dataTestId).click();
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
