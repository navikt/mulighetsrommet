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
  static FNR_PROP = 'data-fnr';

  static get observedAttributes() {
    return [Arbeidsmarkedstiltak.FNR_PROP];
  }

  /**
   * Blir satt etter at applikasjonen har mountet.
   *
   * Ved endringer på `FNR_PROP`-attributtet så blir denne funksjonen kalt slik at vi får
   * propagert endringene til resten av applikasjonen.
   */
  setFnr?: (fnr: string) => void;

  connectedCallback() {
    // This will be app entry point
    const appRoot = document.createElement('div');
    appRoot.id = APPLICATION_WEB_COMPONENT_NAME;

    // The ShadowRoot is rendered separately from the main DOM tree, ensuring that styling
    // does not bleed across trees
    const shadowRoot = this.attachShadow({ mode: 'closed' });
    shadowRoot.appendChild(appRoot);

    this.loadStyles(shadowRoot);

    const fnr = this.getAttribute(Arbeidsmarkedstiltak.FNR_PROP);

    const root = createRoot(appRoot);
    root.render(
      <AppContext fnr={fnr} setFnrRef={setFnr => (this.setFnr = setFnr)}>
        <App />
      </AppContext>
    );
  }

  attributeChangedCallback(name: string, oldValue: string, newValue: string) {
    if (name === Arbeidsmarkedstiltak.FNR_PROP && this.setFnr) {
      this.setFnr(newValue);
    }
  }

  loadStyles(shadowRoot: ShadowRoot) {
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
        // TODO better error handling
        // eslint-disable-next-line no-console
        console.log('ERROR', error);
      });
  }
}

function joinPaths(...paths: (string | null | undefined)[]) {
  return paths.filter(path => !!path).join('/');
}
