import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { NotificationStatus } from "@mr/api-client-v2";
import { Button, HStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useMemo } from "react";
import { useFetcher } from "react-router";
import {
  lesteNotifikasjonerQuery,
  ulesteNotifikasjonerQuery,
} from "../../pages/arbeidsbenk/notifikasjoner/notifikasjonerQueries";
import { EmptyState } from "./EmptyState";
import { Notifikasjonssrad } from "./Notifikasjonsrad";

interface Props {
  lest: boolean;
}

export function Notifikasjonsliste({ lest }: Props) {
  const { data: leste } = useQuery(lesteNotifikasjonerQuery);
  const { data: uleste } = useQuery(ulesteNotifikasjonerQuery);

  const notifikasjoner = useMemo(
    () => (lest ? leste?.data.data : uleste?.data.data),
    [lest, leste, uleste],
  );
  const fetcher = useFetcher();

  function toggleMarkertSomlestUlest() {
    if (notifikasjoner) {
      const newStatus = lest ? NotificationStatus.NOT_DONE : NotificationStatus.DONE;
      const formData = new FormData();
      notifikasjoner.forEach(({ id }) => {
        formData.append("ids[]", id);
        formData.append("statuses[]", newStatus);
      });
      fetcher.submit(formData, { method: "POST", action: `/arbeidsbenk/notifikasjoner` });
    }
  }

  if (notifikasjoner?.length === 0) {
    return (
      <EmptyState
        tittel={lest ? "Du har ingen tidligere notifikasjoner" : "Ingen nye notifikasjoner"}
        beskrivelse={
          lest
            ? "Når du har gjort en oppgave eller lest en beskjed havner de her"
            : "Vi varsler deg når noe skjer"
        }
      />
    );
  }

  return (
    <ReloadAppErrorBoundary>
      <div className="max-w-[1440px] mt-5">
        <HStack align={"end"} justify={"end"}>
          <Button variant="tertiary-neutral" size="small" onClick={toggleMarkertSomlestUlest}>
            Merk alle som {lest ? "ulest" : "lest"}
          </Button>
        </HStack>
        <ul className="m-0 mb-4 pl-0 flex flex-col">
          {notifikasjoner?.map((n) => {
            return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
          })}
        </ul>
      </div>
    </ReloadAppErrorBoundary>
  );
}
