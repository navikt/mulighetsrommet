import { useCallback } from "react";

type HendelseTyper = "KNAPP_KLIKKET" | "SKJEMA_FULLFØRT" | "MODAL_ÅPNET" | "FANE_BYTTET";

type HendelseDataMap = {
  KNAPP_KLIKKET: { tekst: string; sidenavn: string };
  SKJEMA_FULLFØRT: { skjemaType: string };
  MODAL_ÅPNET: { modalType: string };
  FANE_BYTTET: { tekst: string; sidenavn: string; fraFane: string };
};

type UmamiEvent = {
  [K in HendelseTyper]: { type: K } & HendelseDataMap[K];
}[HendelseTyper];

export function useUmami() {
  const logUmamiHendelse = useCallback((event: UmamiEvent) => {
    const { type, ...data } = event;
    if (typeof window !== "undefined" && window.umami) {
      window.umami.track(type, {
        appnavn: "mr-adminflate",
        ...data,
      });
    }
  }, []);

  return { logUmamiHendelse };
}
