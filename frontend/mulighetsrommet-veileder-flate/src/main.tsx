import "./polyfill";
import { createRoot } from "react-dom/client";
import { APPLICATION_NAME, APPLICATION_WEB_COMPONENT_NAME } from "./constants";
import React from "react";
import { Arbeidsmarkedstiltak } from "./WebComponentWrapper";
import { App } from "./App";
import { AppContext } from "./AppContext";

if (import.meta.env.VITE_MULIGHETSROMMET_API_MOCK === "true") {
  import("./mock/worker")
    .then(({ initializeMockServiceWorker }) => {
      return initializeMockServiceWorker();
    })
    .then(render)
    .catch((error) => {
      // eslint-disable-next-line no-console
      console.error("Error occurred while initializing MSW", error);
    });
} else {
  render();
}

/**
 * Applikasjonen blir lastet inn i `veilarbpersonflate` i dev og prod ved at vi definerer et
 * custom HTMLElement med navnet `APPLICATION_WEB_COMPONENT_NAME`, se Web Components for mer info [0].
 * Dette lar oss enkapsulere stylingen til applikasjonen slik at vi slipper css-bleed på
 * tvers av applikasjoner i `veilarbpersonflate`.
 *
 * Når vi kjører applikasjonen lokalt sjekker vi eksplisitt om det finnes en node med navnet
 * `APPLICATION_NAME` før vi rendrer applikasjonen fordi denne noden er definert i `index.html`
 * (men ikke i `veilarbpersonflate`).
 *
 * [0]: https://developer.mozilla.org/en-US/docs/Web/API/Web_components
 */
function render() {
  const demoContainer = document.getElementById(APPLICATION_NAME);
  if (import.meta.env.DEV && demoContainer) {
    const root = createRoot(demoContainer);
    root.render(
      <AppContext contextData={{ fnr: "12345678910", enhet: "0315" }}>
        <App />
      </AppContext>,
    );
  } else if (demoContainer) {
    customElements.define(APPLICATION_WEB_COMPONENT_NAME, Arbeidsmarkedstiltak);

    const root = createRoot(demoContainer);
    root.render(
      React.createElement(APPLICATION_WEB_COMPONENT_NAME, {
        "data-fnr": import.meta.env.VITE_DEMO_FNR ?? null,
        "data-enhet": import.meta.env.VITE_DEMO_ENHET ?? null,
      }),
    );
  } else {
    customElements.define(APPLICATION_WEB_COMPONENT_NAME, Arbeidsmarkedstiltak);
  }
}
