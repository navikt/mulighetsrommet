import { matchRoutes, type RouteObject } from "react-router";
import { umamiEventSchemas } from "./useUmami";

type Payload = Record<string, unknown>;

export type InnblikkBeforeSend = (type: string, payload: Payload) => Payload | false;

function filterEventData(name: unknown, value: unknown): Payload | false {
  if (
    typeof name !== "string" ||
    !Object.hasOwn(umamiEventSchemas, name) ||
    !value ||
    typeof value !== "object" ||
    Array.isArray(value)
  ) {
    return false;
  }
  const schema = umamiEventSchemas[name as keyof typeof umamiEventSchemas];
  const result = schema.safeParse({ ...value });
  return result.success ? result.data : false;
}

export function createInnblikkBeforeSend(
  routes: RouteObject[],
  origin: string,
): InnblikkBeforeSend {
  function normalizeUrl(value: unknown): string | undefined {
    if (typeof value !== "string" || !value) return undefined;
    try {
      const url = new URL(value, origin);
      if (url.origin !== origin) return undefined;
      // Reject malformed escapes before React Router can log the original path.
      decodeURIComponent(url.pathname);
      const matches = matchRoutes(routes, url.pathname);
      if (!matches) return undefined;

      const pattern =
        matches
          .reduce((path, { route }) => {
            if (!route.path) return path;
            return route.path.startsWith("/") ? route.path : `${path}/${route.path}`;
          }, "")
          .replace(/\/+/g, "/")
          .replace(/\/$/, "") || "/";

      return new URL(pattern, origin).href;
    } catch {
      return undefined;
    }
  }

  return (type, payload) => {
    try {
      if (type !== "event" || payload.id !== undefined) {
        return false;
      }

      const url = normalizeUrl(payload.url);
      if (!url) return false;

      const pageView = {
        website: typeof payload.website === "string" ? payload.website : undefined,
        hostname: new URL(origin).hostname,
        screen: typeof payload.screen === "string" ? payload.screen : undefined,
        language: typeof payload.language === "string" ? payload.language : undefined,
        url,
        title: new URL(url).pathname,
        referrer: normalizeUrl(payload.referrer),
      };

      const isCustomEvent = payload.name !== undefined;
      let eventData: Payload | false = false;
      if (isCustomEvent) {
        if (
          payload.tag !== undefined ||
          Object.keys(payload).some(
            (key) => !Object.hasOwn(pageView, key) && !["name", "data", "tag", "id"].includes(key),
          )
        ) {
          return false;
        }
        eventData = filterEventData(payload.name, payload.data);
        if (!eventData) return false;
      } else if (payload.data !== undefined) {
        return false;
      }

      return isCustomEvent ? { ...pageView, name: payload.name, data: eventData } : pageView;
    } catch {
      return false;
    }
  };
}

export function initializeInnblikk(routes: RouteObject[], browser: Window): void {
  const filter = createInnblikkBeforeSend(routes, browser.location.origin);
  let lastPathname: string | undefined;
  let previousUrl: unknown;

  browser.innblikkBeforeSend = (type, payload) => {
    try {
      const isCustomEvent = payload.name !== undefined;
      const pathname = browser.location.pathname;
      const filtered = filter(type, {
        ...payload,
        // The script's cached URL is stale after back/forward navigation.
        url: browser.location.href,
        referrer: isCustomEvent ? previousUrl : (previousUrl ?? payload.referrer),
      });
      if (isCustomEvent) return filtered;
      if (!filtered) {
        if (type === "event" && payload.data === undefined) {
          lastPathname = undefined;
          previousUrl = undefined;
        }
        return false;
      }
      if (pathname === lastPathname) return false;
      lastPathname = pathname;
      previousUrl = filtered.url;
      return filtered;
    } catch {
      return false;
    }
  };
}

export function trackInnblikkPageView(): void {
  window.umami?.track((payload) => ({ ...payload, url: window.location.href }));
}
