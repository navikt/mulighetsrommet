import { NotificationStatus } from "@mr/api-client";
import { useNotifikasjonerForAnsatt } from "@/api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Laster } from "@/components/laster/Laster";
import { EmptyState } from "./EmptyState";
import { Notifikasjonssrad } from "./Notifikasjonsrad";
import { Button, HStack } from "@navikt/ds-react";
import { useMutateNotifikasjoner } from "@/api/notifikasjoner/useMutateNotifikasjoner";
import { useNavigate } from "react-router";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "@/api/QueryKeys";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";

interface Props {
  lest: boolean;
}

export function Notifikasjonsliste({ lest }: Props) {
  const { isLoading, data: paginertResultat } = useNotifikasjonerForAnsatt(
    lest ? NotificationStatus.DONE : NotificationStatus.NOT_DONE,
  );
  const mutation = useMutateNotifikasjoner();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  function toggleMarkertSomlestUlest() {
    if (paginertResultat) {
      const notifikasjoner = paginertResultat.data.map((n) => ({
        id: n.id,
        status: lest ? NotificationStatus.NOT_DONE : NotificationStatus.DONE,
      }));
      mutation.mutate(
        { notifikasjoner },
        {
          onSuccess: () => {
            navigate(`/notifikasjoner${lest ? "" : "/tidligere"}`);

            return Promise.all([
              queryClient.invalidateQueries({
                queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
              }),
              queryClient.invalidateQueries({
                queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.DONE),
              }),
            ]);
          },
        },
      );
    }
  }

  if (isLoading && !paginertResultat) {
    return <Laster />;
  }

  if (!paginertResultat) {
    return null;
  }

  const { data = [] } = paginertResultat;

  if (data.length === 0) {
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
      <div className="max-w-[1440px]">
        <HStack align={"end"} justify={"end"}>
          <Button variant="tertiary-neutral" size="small" onClick={toggleMarkertSomlestUlest}>
            Merk alle som {lest ? "ulest" : "lest"}
          </Button>
        </HStack>
        <ul className="m-0 mb-4 pl-0 flex flex-col">
          {data.map((n) => {
            return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
          })}
        </ul>
      </div>
    </ReloadAppErrorBoundary>
  );
}
