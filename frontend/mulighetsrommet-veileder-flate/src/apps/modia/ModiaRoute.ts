import { SyntheticEvent } from "react";

type UUID = string;

export enum ModiaRoute {
  DIALOG,
  ARBEIDSMARKEDSTILTAK_DELTAKELSE,
  ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE,
}

export type ModiaRouteParams =
  | {
      route: ModiaRoute.DIALOG;
      dialogId: string | number;
    }
  | {
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE;
      gjennomforingId: UUID;
    }
  | {
      route: ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE;
      deltakerId: UUID;
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
    case ModiaRoute.ARBEIDSMARKEDSTILTAK_OPPRETT_DELTAKELSE:
      return `/arbeidsmarkedstiltak/deltakelse/${route.gjennomforingId}`;
    case ModiaRoute.ARBEIDSMARKEDSTILTAK_DELTAKELSE:
      return `/arbeidsmarkedstiltak/deltakelse/deltaker/${route.deltakerId}`;
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
