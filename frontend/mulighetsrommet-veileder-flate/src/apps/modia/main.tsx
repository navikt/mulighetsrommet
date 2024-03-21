import React from "react";
import { createRoot } from "react-dom/client";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { APPLICATION_NAME, APPLICATION_WEB_COMPONENT_NAME } from "@/constants";
import { ModiaArbeidsmarkedstiltakWrapper } from "./ModiaArbeidsmarkedstiltakWrapper";
import { initAmplitudeModia } from "@/logging/amplitude";
import "../../index.css";

if (import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    app: {
      name: "mulighetsrommet-veileder-flate",
    },
  });
}

initAmplitudeModia();

/**
 * Applikasjonen blir lastet inn i `veilarbpersonflate` i dev og prod ved at vi definerer et
 * custom HTMLElement med navnet `APPLICATION_WEB_COMPONENT_NAME`, se Web Components for mer info [0].
 * Dette lar oss enkapsulere stylingen til applikasjonen slik at vi slipper css-bleed på
 * tvers av applikasjoner i `veilarbpersonflate`.
 *
 * [0]: https://developer.mozilla.org/en-US/docs/Web/API/Web_components
 */
customElements.define(APPLICATION_WEB_COMPONENT_NAME, ModiaArbeidsmarkedstiltakWrapper);

/**
 * Må kjøres via `vite build` og `vite preview` (altså ikke via `vite dev`) for at styling under
 * shadow root skal bli lastet riktig.
 */
const container = document.getElementById(APPLICATION_NAME);
if (container) {
  const root = createRoot(container);
  const app = React.createElement(APPLICATION_WEB_COMPONENT_NAME, {
    "data-fnr": import.meta.env.VITE_DEMO_FNR ?? null,
    "data-enhet": import.meta.env.VITE_DEMO_ENHET ?? null,
  });
  root.render(app);
}
