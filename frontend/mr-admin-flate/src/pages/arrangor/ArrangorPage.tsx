import { Accordion } from "@navikt/ds-react";
import { useState } from "react";
import { useArrangorHovedenhet } from "../../api/arrangor/useArrangorHovedenhet";
import { ArrangorKort } from "../../components/arrangor/ArrangorKort";
import { ArrangorIkon } from "../../components/ikoner/ArrangorIkon";
import { Brodsmule, Brodsmuler } from "../../components/navigering/Brodsmuler";
import { ContainerLayout } from "../../layouts/ContainerLayout";
import { HeaderBanner } from "../../layouts/HeaderBanner";
import styles from "./ArrangorPage.module.scss";

interface Props {
  arrangorId: string;
}

export function ArrangorPage({ arrangorId }: Props) {
  const { data: arrangor, isLoading } = useArrangorHovedenhet(arrangorId);
  const [openHovedenhet, setOpenHovedenhet] = useState(true); // TODO Fiks default open for arrangører med underenheter

  const brodsmuler: Brodsmule[] = [
    { tittel: "Forside", lenke: "/" },
    { tittel: "Arrangører", lenke: "/arrangorer" },
    { tittel: `${arrangor?.navn}`, lenke: `/arrangorer/${arrangorId}` },
  ];

  if (!arrangor || isLoading) return null;

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <main>
        <HeaderBanner heading={arrangor.navn} ikon={<ArrangorIkon />} />
        <ContainerLayout>
          <Accordion className={styles.container}>
            {!arrangor.overordnetEnhet ? (
              <Accordion.Item open={openHovedenhet}>
                <Accordion.Header onClick={() => setOpenHovedenhet(!openHovedenhet)}>
                  Arrangør - {arrangor.organisasjonsnummer} {arrangor.navn}
                </Accordion.Header>
                <Accordion.Content>
                  <ArrangorKort arrangor={arrangor} />
                </Accordion.Content>
              </Accordion.Item>
            ) : null}
          </Accordion>
        </ContainerLayout>
      </main>
    </>
  );
}
