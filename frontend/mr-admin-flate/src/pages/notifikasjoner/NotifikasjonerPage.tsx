import { Heading, Tabs } from "@navikt/ds-react";
import { useAtom } from "jotai/index";
import { logEvent } from "mulighetsrommet-veileder-flate/src/core/api/logger";
import { kebabCase } from "mulighetsrommet-veileder-flate/src/utils/Utils";
import { faneAtom } from "../../api/atoms";
import { Notifikasjonsliste } from "../../components/notifikasjoner/Notifikasjonsliste";
import styles from "./NotifikasjonerPage.module.scss";

export function NotifikasjonerPage() {
  const [fane, setFane] = useAtom(faneAtom);

  const faneoverskrifter = [
    "Nye notifikasjoner",
    "Tidligere notifikasjoner",
  ] as const;
  const tabValueTilFaneoverskrifter: { [key: string]: string } = {
    tab_notifikasjoner_1: faneoverskrifter[0],
    tab_notifikasjoner_2: faneoverskrifter[1],
  };

  return (
    <main className={styles.notifikasjoner}>
      <Heading level="2" size="large" className={styles.heading}>
        Notifikasjoner
      </Heading>
      <Tabs
        defaultValue={fane}
        size="small"
        selectionFollowsFocus
        className={styles.fane_root}
        onChange={(value) => {
          logEvent("mulighetsrommet.faner", {
            value: tabValueTilFaneoverskrifter[value],
          });
          setFane(value);
        }}
      >
        <Tabs.List className={styles.fane_liste} id="fane_liste">
          {faneoverskrifter.map((fane, index) => (
            <Tabs.Tab
              key={index}
              value={`tab_notifikasjoner_${index + 1}`}
              label={fane}
              className={styles.btn_tab}
              data-testid={`fane_${kebabCase(fane)}`}
            />
          ))}
        </Tabs.List>
        <div className={styles.fane_panel}>
          <Tabs.Panel value="tab_notifikasjoner_1">
            <Notifikasjonsliste lest={false} />
          </Tabs.Panel>
          <Tabs.Panel value="tab_notifikasjoner_2">
            <Notifikasjonsliste lest={true} />
          </Tabs.Panel>
        </div>
      </Tabs>
    </main>
  );
}
