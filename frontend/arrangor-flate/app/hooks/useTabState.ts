import { useSearchParams } from "react-router";
import { useEffect } from "react";

export type Tabs = "aktive" | "historiske" | "tilsagnsoversikt";

export function getTabStateOrDefault(url: URL): Tabs {
  const val = url.searchParams.get("forside-tab") || "";
  return convertToTabOrDefault(val);
}

function convertToTabOrDefault(str: string | null): Tabs {
  if (str === "aktive" || str === "historiske" || str === "tilsagnsoversikt") {
    return str;
  }
  return "aktive";
}

export function useTabState(
  key: "forside-tab" = "forside-tab",
  defaultValue: Tabs = "aktive",
): [string, (tab: Tabs) => void] {
  const [searchParams, setSearchParams] = useSearchParams();

  const currentTab = convertToTabOrDefault(searchParams.get(key));

  const setTab = (newTab: Tabs) => {
    setSearchParams({ [key]: newTab });
  };

  useEffect(() => {
    if (!searchParams.has(key)) {
      setSearchParams({ [key]: defaultValue });
    }
  }, [key, defaultValue, searchParams, setSearchParams]);

  return [currentTab, setTab];
}
