import { Tiltakskode } from "@api-client";
import { SyntheticEvent } from "react";

type UUID = string;

export enum ModiaRoute {
  DIALOG,
  ARBEIDSMARKEDSTILTAK_DELTAKELSE,
  ARBEIDSMARKEDSTILTAK_DELTAKELSE_PAMELDING,
  ARBEIDSMARKEDSTILTAK_DELTAKELSE_OPPRETT_ENKELTPLASS,
}

export type ModiaRouteParams =
  | {
      route: ModiaRoute.DIALOG;
      dialogId: string | number;
    }
  | {
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_PAMELDING;
      gjennomforingId: UUID;
    }
  | {
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE;
      deltakerId: UUID;
    }
  | {
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_OPPRETT_ENKELTPLASS;
      tiltakskode: Tiltakskode;
    };

export function resolveModiaRoute(route: ModiaRouteParams) {
  const href = resolveRoutePath(route);

  const navigate = (event?: Event | SyntheticEvent) => {
    event?.stopPropagation();
    emitVeilarbpersonflateNavigateEvent(href);
  };

  return { href, navigate };
}

export function resolveRoutePath(route: ModiaRouteParams): string {
  switch (route.route) {
    case ModiaRoute.DIALOG:
      return `/dialog/${route.dialogId}`;
    case ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE:
      return `/arbeidsmarkedstiltak/deltakelse/deltaker/${route.deltakerId}`;
    case ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_PAMELDING:
      return `/arbeidsmarkedstiltak/deltakelse/${route.gjennomforingId}`;
    case ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE_OPPRETT_ENKELTPLASS:
      return `/arbeidsmarkedstiltak/deltakelse/tiltak/${route.tiltakskode}`;
  }
}

export function navigateToModiaApp(route: ModiaRouteParams) {
  const path = resolveRoutePath(route);
  emitVeilarbpersonflateNavigateEvent(path);
}

function emitVeilarbpersonflateNavigateEvent(path: string) {
  window.history.pushState(null, "", path);
  window.dispatchEvent(new CustomEvent("veilarbpersonflate.navigate"));
}
