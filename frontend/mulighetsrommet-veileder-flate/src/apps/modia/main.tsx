import { APPLICATION_WEB_COMPONENT_NAME } from "../../constants";
import { initAmplitude } from "../../logging/amplitude";
import { getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { ModiaArbeidsmarkedstiltakWrapper } from "./ModiaArbeidsmarkedstiltakWrapper";

if (import.meta.env.PROD && import.meta.env.VITE_FARO_URL) {
  initializeFaro({
    url: import.meta.env.VITE_FARO_URL,
    instrumentations: [...getWebInstrumentations({ captureConsole: true })],
    app: {
      name: "mulighetsrommet-veileder-flate",
    },
  });

  initAmplitude();
}

/**
 * Applikasjonen blir lastet inn i `veilarbpersonflate` i dev og prod ved at vi definerer et
 * custom HTMLElement med navnet `APPLICATION_WEB_COMPONENT_NAME`, se Web Components for mer info [0].
 * Dette lar oss enkapsulere stylingen til applikasjonen slik at vi slipper css-bleed på
 * tvers av applikasjoner i `veilarbpersonflate`.
 *
 * [0]: https://developer.mozilla.org/en-US/docs/Web/API/Web_components
 */
customElements.define(APPLICATION_WEB_COMPONENT_NAME, ModiaArbeidsmarkedstiltakWrapper);
