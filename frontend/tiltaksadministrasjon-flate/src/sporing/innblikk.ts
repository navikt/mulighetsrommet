import { matchRoutes, type RouteObject } from "react-router";
import { umamiEventSchemas } from "./useUmami";

type Payload = Record<string, unknown>;

export type InnblikkBeforeSend = (
  type: string,
  payload: Payload,
) => (Payload & { url: string }) | false;

function filterEventData(name: unknown, value: unknown): Payload | false {
  if (typeof name !== "string" || !Object.hasOwn(umamiEventSchemas, name)) {
    return false;
  }
  const schema = umamiEventSchemas[name as keyof typeof umamiEventSchemas];
  const result = schema.safeParse(value);
  return result.success ? result.data : false;
}

export function createInnblikkBeforeSend(
  routes: RouteObject[],
  origin: string,
): InnblikkBeforeSend {
  const hostname = new URL(origin).hostname;

  function toRouteUrl(value: unknown): string | undefined {
    if (typeof value !== "string" || !value) return undefined;
    let url: URL;
    try {
      url = new URL(value, origin);
      decodeURIComponent(url.pathname);
    } catch {
      return undefined;
    }
    if (url.origin !== origin) return undefined;
    const matches = matchRoutes(routes, url.pathname);
    if (!matches) return undefined;

    let routePath = "";
    for (const { route } of matches) {
      if (!route.path) continue;
      routePath = route.path.startsWith("/") ? route.path : `${routePath}/${route.path}`;
    }
    const pattern = routePath.replace(/\/+/g, "/").replace(/\/$/, "") || "/";
    return new URL(pattern, origin).href;
  }

  return (type, payload) => {
    if (type !== "event" || payload.id !== undefined) return false;

    const url = toRouteUrl(payload.url);
    if (!url) return false;

    const pageView = {
      website: typeof payload.website === "string" ? payload.website : undefined,
      hostname,
      screen: typeof payload.screen === "string" ? payload.screen : undefined,
      language: typeof payload.language === "string" ? payload.language : undefined,
      url,
      title: new URL(url).pathname,
      referrer: toRouteUrl(payload.referrer),
    };

    if (payload.name === undefined) {
      return payload.data === undefined ? pageView : false;
    }

    const data = filterEventData(payload.name, payload.data);
    return data ? { ...pageView, name: payload.name, data } : false;
  };
}

export function initializeInnblikk(routes: RouteObject[], browser: Window): void {
  const preparePayload = createInnblikkBeforeSend(routes, browser.location.origin);
  let lastTrackedPathname: string | undefined;
  let previousPageUrl: string | undefined;

  browser.innblikkBeforeSend = (type, payload) => {
    const isCustomEvent = payload.name !== undefined;
    const { pathname, href } = browser.location;
    const prepared = preparePayload(type, {
      ...payload,
      url: href,
      referrer: isCustomEvent ? previousPageUrl : (previousPageUrl ?? payload.referrer),
    });
    if (isCustomEvent) return prepared;

    if (!prepared) {
      if (type === "event" && payload.data === undefined) {
        lastTrackedPathname = undefined;
        previousPageUrl = undefined;
      }
      return false;
    }
    if (pathname === lastTrackedPathname) return false;

    lastTrackedPathname = pathname;
    previousPageUrl = prepared.url;
    return prepared;
  };
}

export function trackInnblikkPageView(): void {
  window.umami?.track((payload) => ({ ...payload, url: window.location.href }));
}
