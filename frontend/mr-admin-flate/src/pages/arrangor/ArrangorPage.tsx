import { useArrangorHovedenhet } from "@/api/arrangor/useArrangorHovedenhet";
import { ArrangorKort } from "@/components/arrangor/ArrangorKort";
import { ArrangorIkon } from "@/components/ikoner/ArrangorIkon";
import { Brodsmule, Brodsmuler } from "@/components/navigering/Brodsmuler";
import { ContentBox } from "@/layouts/ContentBox";
import { HeaderBanner } from "@/layouts/HeaderBanner";
import { Accordion } from "@navikt/ds-react";
import { useState } from "react";
import { useParams } from "react-router";

export function ArrangorPage() {
  const { arrangorId } = useParams();
  if (!arrangorId) {
    throw Error("Fant ikke arrangorId i route");
  }
  const { data: arrangor, isLoading } = useArrangorHovedenhet(arrangorId);
  const [openHovedenhet, setOpenHovedenhet] = useState(true); // TODO Fiks default open for arrangører med underenheter

  const brodsmuler: Brodsmule[] = [
    { tittel: "Arrangører", lenke: "/arrangorer" },
    { tittel: `${arrangor?.navn}`, lenke: `/arrangorer/${arrangorId}` },
  ];

  if (!arrangor || isLoading) return null;

  return (
    <>
      <Brodsmuler brodsmuler={brodsmuler} />
      <HeaderBanner heading={arrangor.navn} ikon={<ArrangorIkon />} />
      <ContentBox>
        <Accordion className="bg-white">
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
      </ContentBox>
    </>
  );
}
