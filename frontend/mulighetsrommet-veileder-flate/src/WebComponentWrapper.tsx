import React from "react";
import { createRoot } from "react-dom/client";
import { APPLICATION_WEB_COMPONENT_NAME } from "./constants";
import { App } from "./App";
import { AppContext } from "./AppContext";
import urlJoin from "url-join";

interface ViteAssetManifest {
  "index.html": {
    css: string[];
  };
}

export class Arbeidsmarkedstiltak extends HTMLElement {
  static FNR_PROP = "data-fnr";

  private readonly root: HTMLDivElement;

  constructor() {
    super();

    // This will be app entry point
    this.root = document.createElement("div");
    this.root.id = APPLICATION_WEB_COMPONENT_NAME;
  }

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
    // The ShadowRoot is rendered separately from the main DOM tree, ensuring that styling
    // does not bleed across trees
    const shadowRoot = this.attachShadow({ mode: "open" });
    shadowRoot.appendChild(this.root);

    this.loadStyles(shadowRoot)
      .then(() => {
        const fnr = this.getAttribute(Arbeidsmarkedstiltak.FNR_PROP);
        this.renderApp(fnr);
      })
      .catch((error) => {
        this.displayError(error?.message ?? error);
      });
  }

  attributeChangedCallback(name: string, oldValue: string, newValue: string) {
    if (name === Arbeidsmarkedstiltak.FNR_PROP && this.setFnr) {
      this.setFnr(newValue);
    }
  }

  async loadStyles(shadowRoot: ShadowRoot) {
    const response = await fetch(urlJoin(import.meta.env.BASE_URL, "asset-manifest.json"));
    if (!response.ok) {
      throw Error(`Failed to get resource '${response.url}'`);
    }

    const manifest: ViteAssetManifest = await response.json();
    for (const css of manifest["index.html"].css) {
      const link = document.createElement("link");
      link.rel = "stylesheet";
      link.href = urlJoin(import.meta.env.BASE_URL, css);

      shadowRoot.appendChild(link);
    }
  }

  renderApp(fnr: string | null) {
    const root = createRoot(this.root);
    root.render(
      <AppContext fnr={fnr} setFnrRef={(setFnr) => (this.setFnr = setFnr)}>
        <App />
      </AppContext>,
    );
  }

  displayError(error: string | Error) {
    this.root.innerHTML = `<p>${error}</p>`;
  }
}
