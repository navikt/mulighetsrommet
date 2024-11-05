import { Tabs } from "../routes/$orgnr_.refusjonskrav";

export function getCurrentTab(request: Request): Tabs {
  return (new URL(request.url).searchParams.get("tab") as Tabs) || "aktive";
}
