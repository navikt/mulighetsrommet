import { Tabs } from "../routes/$orgnr_.utbetaling";

export function getCurrentTab(request: Request): Tabs {
  return (new URL(request.url).searchParams.get("forside-tab") as Tabs) || "aktive";
}
