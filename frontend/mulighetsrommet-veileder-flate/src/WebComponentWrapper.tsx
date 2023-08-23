import dsStyles from '@navikt/ds-css/dist/index.css?inline';
import React from 'react';
import App from './App';
import { createRoot } from 'react-dom/client';
import { ARBEIDSMARKEDSTILTAK } from './constants';

export class Arbeidsmarkedstiltak extends HTMLElement {
  setFnr?: (fnr: string) => void;
  connectedCallback() {
    // Cant mount on shadowRoot, create a extra div for mounting modal
    const shadowDomFirstChild = document.createElement('div');
    // This will be app entry point, need to be outside modal-mount node
    const appRoot = document.createElement('div');
    appRoot.id = ARBEIDSMARKEDSTILTAK;
    const shadowRoot = this.attachShadow({ mode: 'closed' });
    shadowRoot.appendChild(shadowDomFirstChild);
    shadowDomFirstChild.appendChild(appRoot);

    // Load styles under this shadowDom-node, not root element
    const styleElem = document.createElement('style');
    styleElem.innerHTML = dsStyles;
    shadowRoot.appendChild(styleElem);
    //TODO import resten av stilene

    const fnr = this.getAttribute('data-fnr');

    if (!fnr) {
      return null;
    }

    window.localStorage.setItem('data-fnr', fnr);

    // ReactDOM.render(
    //   <ModalProvider appElement={appRoot} rootElement={shadowDomFirstChild}>
    //TODO fiks setFnr
    //     <Provider key={fnr} fnr={fnr} setFnrRef={(setFnr) => (this.setFnr = setFnr)}>
    //       <App Routes={Routes} key={'1'} />
    //     </Provider>
    //   </ModalProvider>,
    //   appRoot
    // );

    const root = createRoot(appRoot);
    root.render(<App fnr={fnr} />);
  }

  attributeChangedCallback(name: string, oldValue: string, newValue: string) {
    if (name === 'data-fnr' && this.setFnr) {
      window.localStorage.setItem('data-fnr', newValue);
      this.setFnr(newValue);
    }
  }

  static get observedAttributes() {
    return ['data-fnr'];
  }
}
