import React from 'react';
import { createRoot } from 'react-dom/client';
import { APPLICATION_WEB_COMPONENT_NAME } from './constants';
import { App } from './App';
import { AppContext } from './AppContext';

interface ViteAssetManifest {
  'index.html': {
    css: string[];
  };
}

export class Arbeidsmarkedstiltak extends HTMLElement {
  static get observedAttributes() {
    return ['data-fnr'];
  }

  /**
   * Blir satt etter at applikasjonen har mountet.
   *
   * Ved endringer på "data-fnr"-attributtet så blir denne funksjonen kalt slik at vi får
   * propagert endringene til resten av applikasjonen.
   */
  setFnr?: (fnr: string) => void;

  connectedCallback() {
    // Cant mount on shadowRoot, create a extra div for mounting modal
    const shadowDomFirstChild = document.createElement('div');
    // This will be app entry point, need to be outside modal-mount node
    const appRoot = document.createElement('div');
    appRoot.id = APPLICATION_WEB_COMPONENT_NAME;
    const shadowRoot = this.attachShadow({ mode: 'closed' });
    shadowRoot.appendChild(shadowDomFirstChild);
    shadowDomFirstChild.appendChild(appRoot);

    fetch(joinPaths(import.meta.env.BASE_URL, 'asset-manifest.json'))
      .then(response => {
        if (!response.ok) {
          throw Error(`Failed to get resource '${response.url}'`);
        }

        return response.json();
      })
      .then((manifest: ViteAssetManifest) => {
        for (const css of manifest['index.html'].css) {
          const link = document.createElement('link');
          link.rel = 'stylesheet';
          link.href = joinPaths(import.meta.env.BASE_URL, css);

          shadowRoot.appendChild(link);
        }
      })
      .catch(error => {
        console.log('ERROR', error);
      });

    const fnr = this.getAttribute('data-fnr');

    const root = createRoot(appRoot);
    root.render(
      <AppContext fnr={fnr} setFnrRef={setFnr => (this.setFnr = setFnr)}>
        <App />
      </AppContext>
    );
  }

  attributeChangedCallback(name: string, oldValue: string, newValue: string) {
    if (name === 'data-fnr' && this.setFnr) {
      this.setFnr(newValue);
    }
  }
}

function joinPaths(...paths: (string | null | undefined)[]) {
  return paths.filter(path => !!path).join('/');
}
