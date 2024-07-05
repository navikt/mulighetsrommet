import { PropsWithChildren } from "react";
import { Tabs } from "@navikt/ds-react";
import styles from "./DetaljerTabs.module.scss";

interface Props {
  activeTab: string;
}
export function DetaljerTabs({ activeTab, children }: PropsWithChildren<Props>) {
  return (
    <Tabs defaultValue={activeTab}>
      <Tabs.List className={styles.detaljer_tabs}>{children}</Tabs.List>
    </Tabs>
  );
}
