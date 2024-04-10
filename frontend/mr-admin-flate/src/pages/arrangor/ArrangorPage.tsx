import { Accordion, Heading } from "@navikt/ds-react";
import { Arrangor } from "mulighetsrommet-api-client";
import { useState } from "react";
import { useArrangorHovedenhet } from "../../api/arrangor/useArrangorHovedenhet";
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
  const [openHovedenhet, setOpenHovedenhet] = useState(arrangor?.underenheter?.length === 0);

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
                  Hovedenhet - {arrangor.organisasjonsnummer} {arrangor.navn}
                </Accordion.Header>
                <Accordion.Content>
                  <ArrangorKort arrangor={arrangor} />
                </Accordion.Content>
              </Accordion.Item>
            ) : null}
            {arrangor.underenheter?.map((underenhet) => (
              <Accordion.Item key={underenhet.id}>
                <Accordion.Header>
                  Underenhet - {underenhet.organisasjonsnummer} {underenhet.navn}
                </Accordion.Header>
                <Accordion.Content>
                  <ArrangorKort arrangor={arrangor} />
                </Accordion.Content>
              </Accordion.Item>
            ))}
          </Accordion>
        </ContainerLayout>
      </main>
    </>
  );
}

function ArrangorKort({ arrangor }: { arrangor: Arrangor }) {
  return (
    <div>
      <Heading level="2" size="small">
        {arrangor.navn}
      </Heading>
      <p>Organisasjonsnummer: {arrangor.organisasjonsnummer}</p>
    </div>
  );
}
