import { Heading, Tabs } from "@navikt/ds-react";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { LesteNotifikasjonsliste } from "../../components/notifikasjoner/LesteNotifikasjonsliste";
import { useAtom } from "jotai/index";
import { logEvent } from "mulighetsrommet-veileder-flate/src/core/api/logger";
import { kebabCase } from "mulighetsrommet-veileder-flate/src/utils/Utils";
import { faneAtom } from "../../api/atoms";
import styles from "./NotifikasjonerPage.module.scss";
import { UlesteNotifikasjonsliste } from "../../components/notifikasjoner/UlesteNotifikasjonsliste";

export function NotifikasjonerPage() {
  const [fane, setFane] = useAtom(faneAtom);

  const faneoverskrifter = ["Nye varsler", "Tidligere varsler"] as const;
  const tabValueTilFaneoverSkrifter: { [key: string]: string } = {
    tab1: faneoverskrifter[0],
    tab2: faneoverskrifter[1],
  };

  return (
    <ContainerLayout>
      <Heading size={"medium"}>Varsler</Heading>
      <Tabs
        defaultValue={fane}
        size="small"
        selectionFollowsFocus
        className={styles.fane_root}
        onChange={(value) => {
          logEvent("mulighetsrommet.faner", {
            value: tabValueTilFaneoverSkrifter[value],
          });
          setFane(value);
        }}
      >
        <Tabs.List className={styles.fane_liste} id="fane_liste">
          {faneoverskrifter.map((fane, index) => (
            <Tabs.Tab
              key={index}
              value={`tab${index + 1}`}
              label={fane}
              className={styles.btn_tab}
              data-testid={`fane_${kebabCase(fane)}`}
            />
          ))}
        </Tabs.List>
        <div className={styles.fane_panel}>
          <Tabs.Panel value="tab1" data-testid="tab1">
            <LesteNotifikasjonsliste />
          </Tabs.Panel>
          <Tabs.Panel value="tab2" data-testid="tab2">
            <UlesteNotifikasjonsliste />
          </Tabs.Panel>
        </div>
      </Tabs>
    </ContainerLayout>
  );
}
