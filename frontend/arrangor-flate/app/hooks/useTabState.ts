import { useSearchParams } from "@remix-run/react";
import { useEffect } from "react";
import { Tabs } from "../routes/$orgnr_.refusjonskrav";

export function useTabState(
  key = "tab",
  defaultValue: Tabs = "aktive",
): [string, (tab: Tabs) => void] {
  const [searchParams, setSearchParams] = useSearchParams();

  const currentTab = searchParams.get(key) || defaultValue;

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
