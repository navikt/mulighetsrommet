import { NotificationStatus } from "mulighetsrommet-api-client";
import { useNotifikasjonerForAnsatt } from "@/api/notifikasjoner/useNotifikasjonerForAnsatt";
import { Laster } from "../laster/Laster";
import { EmptyState } from "./EmptyState";
import styles from "./Notifikasjoner.module.scss";
import { Notifikasjonssrad } from "./Notifikasjonsrad";
import { ReloadAppErrorBoundary } from "mulighetsrommet-frontend-common/components/error-handling/ErrorBoundary";
import { Button, HStack } from "@navikt/ds-react";
import { useMutateNotifikasjoner } from "../../api/notifikasjoner/useMutateNotifikasjoner";
import { useNavigate } from "react-router-dom";
import { useQueryClient } from "@tanstack/react-query";
import { QueryKeys } from "../../api/QueryKeys";

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
            queryClient.invalidateQueries({
              queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.NOT_DONE),
            });
            queryClient.invalidateQueries({
              queryKey: QueryKeys.notifikasjonerForAnsatt(NotificationStatus.DONE),
            });
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
      <div className={styles.max_width}>
        <HStack align={"end"} justify={"end"}>
          <Button variant="tertiary-neutral" size="small" onClick={toggleMarkertSomlestUlest}>
            Merk alle som {lest ? "ulest" : "lest"}
          </Button>
        </HStack>
        <ul className={styles.notifikasjonsliste_ul}>
          {data.map((n) => {
            return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
          })}
        </ul>
      </div>
    </ReloadAppErrorBoundary>
  );
}
