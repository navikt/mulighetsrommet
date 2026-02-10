import { ModiaContext } from "@/apps/modia/ModiaContext";
import { PreviewArbeidsmarkedstiltak } from "@/apps/nav/PreviewArbeidsmarkedstiltak";
import { APPLICATION_WEB_COMPONENT_NAME } from "@/constants";
import createCache from "@emotion/cache";
import { createRoot, Root } from "react-dom/client";
import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router";
import { CustomEmotionCacheProvider } from "./CustomEmotionCacheProvider";
import { ModiaArbeidsmarkedstiltak } from "./ModiaArbeidsmarkedstiltak";

export class ModiaArbeidsmarkedstiltakWrapper extends HTMLElement {
  static FNR_PROP = "data-fnr";
  static ENHET_PROP = "data-enhet";
  static BASE_URL_PROP = "data-base-url";
  static ASSET_MANIFEST_PROP = "data-asset-manifest";

  private readonly root: HTMLDivElement;
  private reactRoot?: Root;
  private baseUrl: string | null = null;
  private assetManifest: string | null = null;

  constructor() {
    super();

    // This will be app entry point
    this.root = document.createElement("div");
    this.root.id = APPLICATION_WEB_COMPONENT_NAME;
  }

  static get observedAttributes() {
    return [
      ModiaArbeidsmarkedstiltakWrapper.FNR_PROP,
      ModiaArbeidsmarkedstiltakWrapper.ENHET_PROP,
      ModiaArbeidsmarkedstiltakWrapper.BASE_URL_PROP,
      ModiaArbeidsmarkedstiltakWrapper.ASSET_MANIFEST_PROP,
    ];
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

    this.tryMountApp();
  }

  disconnectedCallback() {
    this.reactRoot?.unmount();
  }

  attributeChangedCallback(name: string, _oldValue: string, newValue: string) {
    if (name === ModiaArbeidsmarkedstiltakWrapper.FNR_PROP && this.updateContextData) {
      this.updateContextData("fnr", newValue);
    } else if (name === ModiaArbeidsmarkedstiltakWrapper.ENHET_PROP && this.updateContextData) {
      this.updateContextData("enhet", newValue);
    } else if (name === ModiaArbeidsmarkedstiltakWrapper.BASE_URL_PROP) {
      this.baseUrl = newValue;
      this.tryMountApp();
    } else if (name === ModiaArbeidsmarkedstiltakWrapper.ASSET_MANIFEST_PROP) {
      this.assetManifest = newValue;
      this.tryMountApp();
    }
  }

  tryMountApp() {
    const baseUrl = this.baseUrl;
    if (baseUrl === null) {
      return;
    }

    const assetManifest = this.assetManifest;
    if (assetManifest === null) {
      return;
    }

    const shadowRoot = this.shadowRoot;
    if (shadowRoot === null) {
      return;
    }

    this.loadStyles(shadowRoot, baseUrl, assetManifest)
      .then(() => {
        const fnr = this.getAttribute(ModiaArbeidsmarkedstiltakWrapper.FNR_PROP) ?? undefined;
        const enhet = this.getAttribute(ModiaArbeidsmarkedstiltakWrapper.ENHET_PROP) ?? undefined;
        return this.renderApp(fnr, enhet);
      })
      .catch((error) => {
        this.displayError(error?.message ?? `Error loading styles: ${error}`);
      });
  }

  async loadStyles(shadowRoot: ShadowRoot, baseUrl: string, assetManifest: string): Promise<void> {
    const manifest: ViteAssetManifest = JSON.parse(assetManifest);

    const loadedCss = manifest["index.html"].css.map((css) => {
      return new Promise<void>((resolve) => {
        const link = document.createElement("link");
        link.rel = "stylesheet";
        link.href = `${baseUrl}/${css}`;
        link.onload = () => resolve();
        shadowRoot.appendChild(link);
      });
    });

    await Promise.all(loadedCss);
  }

  renderApp(fnr?: string, enhet?: string) {
    this.reactRoot = createRoot(this.root);

    const shadowrootCache = createCache({
      key: "shadowroot-cache",
      container: this.root,
      prepend: true,
    });
    this.reactRoot.render(
      <CustomEmotionCacheProvider cache={shadowrootCache}>
        <ModiaContext
          contextData={{ enhet, fnr }}
          updateContextDataRef={(updateContextData) => (this.updateContextData = updateContextData)}
        >
          <Router>
            <Routes>
              <Route path="arbeidsmarkedstiltak/*" element={<ModiaArbeidsmarkedstiltak />} />
              <Route path="preview/*" element={<PreviewArbeidsmarkedstiltak />} />
              <Route path="*" element={<Navigate replace to="/arbeidsmarkedstiltak" />} />
            </Routes>
          </Router>
        </ModiaContext>
      </CustomEmotionCacheProvider>,
    );
  }

  displayError(error: string | Error) {
    this.root.innerHTML = `<p>${error}</p>`;
  }
}

interface ViteAssetManifest {
  "index.html": {
    css: string[];
  };
}
