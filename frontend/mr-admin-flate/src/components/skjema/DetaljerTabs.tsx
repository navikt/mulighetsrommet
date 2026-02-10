import { PropsWithChildren } from "react";
import { Tabs } from "@navikt/ds-react";

interface Props {
  activeTab: string;
}
export function DetaljerTabs({ activeTab, children }: PropsWithChildren<Props>) {
  return (
    <Tabs defaultValue={activeTab}>
      <Tabs.List className="p-4 pb-6 bg-ax-bg-default max-w-360">{children}</Tabs.List>
    </Tabs>
  );
}
