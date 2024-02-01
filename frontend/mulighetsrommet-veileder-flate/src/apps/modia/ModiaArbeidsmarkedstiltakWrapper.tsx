import { BrowserRouter as Router, Navigate, Route, Routes } from "react-router-dom";
import { createRoot, Root } from "react-dom/client";
import createCache from "@emotion/cache";
import { AppContext } from "@/AppContext";
import { APPLICATION_WEB_COMPONENT_NAME } from "@/constants";
import { CustomEmotionCacheProvider } from "./CustomEmotionCacheProvider";
import { ModiaArbeidsmarkedstiltak } from "./ModiaArbeidsmarkedstiltak";
import { PreviewArbeidsmarkedstiltak } from "@/apps/preview/PreviewArbeidsmarkedstiltak";

export class ModiaArbeidsmarkedstiltakWrapper extends HTMLElement {
  static FNR_PROP = "data-fnr";
  static ENHET_PROP = "data-enhet";

  private readonly root: HTMLDivElement;
  private reactRoot?: Root;

  constructor() {
    super();

    // This will be app entry point
    this.root = document.createElement("div");
    this.root.id = APPLICATION_WEB_COMPONENT_NAME;
  }

  static get observedAttributes() {
    return [ModiaArbeidsmarkedstiltakWrapper.FNR_PROP, ModiaArbeidsmarkedstiltakWrapper.ENHET_PROP];
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
        const fnr = this.getAttribute(ModiaArbeidsmarkedstiltakWrapper.FNR_PROP) ?? undefined;
        const enhet = this.getAttribute(ModiaArbeidsmarkedstiltakWrapper.ENHET_PROP) ?? undefined;
        this.renderApp(fnr, enhet);
      })
      .catch((error) => {
        this.displayError(error?.message ?? error);
      });
  }

  disconnectedCallback() {
    this.reactRoot?.unmount();
  }

  attributeChangedCallback(name: string, _oldValue: string, newValue: string) {
    if (name === ModiaArbeidsmarkedstiltakWrapper.FNR_PROP && this.updateContextData) {
      this.updateContextData("fnr", newValue);
    } else if (name === ModiaArbeidsmarkedstiltakWrapper.ENHET_PROP && this.updateContextData) {
      this.updateContextData("enhet", newValue);
    }
  }

  async loadStyles(shadowRoot: ShadowRoot) {
    const style = document.createElement("style");
    style.innerHTML = SHADOW_STYLE;
    shadowRoot.appendChild(style);
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
        <AppContext
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
        </AppContext>
      </CustomEmotionCacheProvider>,
    );
  }

  displayError(error: string | Error) {
    this.root.innerHTML = `<p>${error}</p>`;
  }
}
