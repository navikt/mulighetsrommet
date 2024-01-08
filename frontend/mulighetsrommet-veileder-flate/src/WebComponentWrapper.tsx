import createCache from "@emotion/cache";
import { createRoot } from "react-dom/client";
import urlJoin from "url-join";
import { App } from "./App";
import { AppContext } from "./AppContext";
import { CustomEmotionCacheProvider } from "./CustomEmotionCacheProvider";
import { APPLICATION_WEB_COMPONENT_NAME } from "./constants";
import { headers } from "./core/api/headers";

interface ViteAssetManifest {
  "index.html": {
    css: string[];
  };
}

export class Arbeidsmarkedstiltak extends HTMLElement {
  static FNR_PROP = "data-fnr";
  static ENHET_PROP = "data-enhet";

  private readonly root: HTMLDivElement;

  constructor() {
    super();

    // This will be app entry point
    this.root = document.createElement("div");
    this.root.id = APPLICATION_WEB_COMPONENT_NAME;
  }

  static get observedAttributes() {
    return [Arbeidsmarkedstiltak.FNR_PROP, Arbeidsmarkedstiltak.ENHET_PROP];
  }

  /**
   * Blir satt etter at applikasjonen har mountet.
   *
   * Ved endringer på `FNR_PROP`-attributtet så blir denne funksjonen kalt slik at vi får
   * propagert endringene til resten av applikasjonen.
   */
  updateContextData?: (key: string, value: string) => void;

  connectedCallback() {
    // The ShadowRoot is rendered separately from the main DOM tree, ensuring that styling
    // does not bleed across trees
    const shadowRoot = this.attachShadow({ mode: "open" });
    shadowRoot.appendChild(this.root);

    this.loadStyles(shadowRoot)
      .then(() => {
        const fnr = this.getAttribute(Arbeidsmarkedstiltak.FNR_PROP) ?? undefined;
        const enhet = this.getAttribute(Arbeidsmarkedstiltak.ENHET_PROP) ?? undefined;
        this.renderApp(fnr, enhet);
      })
      .catch((error) => {
        this.displayError(error?.message ?? error);
      });
  }

  attributeChangedCallback(name: string, _oldValue: string, newValue: string) {
    if (name === Arbeidsmarkedstiltak.FNR_PROP && this.updateContextData) {
      this.updateContextData("fnr", newValue);
    } else if (name === Arbeidsmarkedstiltak.ENHET_PROP && this.updateContextData) {
      this.updateContextData("enhet", newValue);
    }
  }

  async loadStyles(shadowRoot: ShadowRoot) {
    const response = await fetch(urlJoin(import.meta.env.BASE_URL, "asset-manifest.json"), {
      headers,
    });

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

  renderApp(fnr?: string, enhet?: string) {
    const root = createRoot(this.root);

    const shadowrootCache = createCache({
      key: "shadowroot-cache",
      container: this.root,
      prepend: true,
    });
    root.render(
      <CustomEmotionCacheProvider cache={shadowrootCache}>
        <AppContext
          contextData={{ enhet, fnr }}
          updateContextDataRef={(updateContextData) => (this.updateContextData = updateContextData)}
        >
          <App />
        </AppContext>
      </CustomEmotionCacheProvider>,
    );
  }

  displayError(error: string | Error) {
    this.root.innerHTML = `<p>${error}</p>`;
  }
}
