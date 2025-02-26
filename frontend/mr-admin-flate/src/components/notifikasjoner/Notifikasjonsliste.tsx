import { useMutateNotifikasjoner } from "@/api/notifikasjoner/useMutateNotifikasjoner";
import { QueryKeys } from "@/api/QueryKeys";
import { ReloadAppErrorBoundary } from "@/ErrorBoundary";
import { NotificationStatus } from "@mr/api-client-v2";
import { Button, HStack } from "@navikt/ds-react";
import { useQueryClient } from "@tanstack/react-query";
import { useMemo } from "react";
import { useLoaderData, useNavigate, useRevalidator } from "react-router";
import { notifikasjonLoader } from "../../pages/arbeidsbenk/notifikasjoner/notifikasjonerLoader";
import { LoaderData } from "../../types/loader";
import { EmptyState } from "./EmptyState";
import { Notifikasjonssrad } from "./Notifikasjonsrad";
interface Props {
  lest: boolean;
}

export function Notifikasjonsliste({ lest }: Props) {
  const { leste, uleste } = useLoaderData<LoaderData<typeof notifikasjonLoader>>();
  const notifikasjoner = useMemo(() => (lest ? leste : uleste), [lest, leste, uleste]);
  const mutation = useMutateNotifikasjoner();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const revalidator = useRevalidator();

  function toggleMarkertSomlestUlest() {
    if (notifikasjoner) {
      const payload = notifikasjoner.data.map(({ id }) => ({
        id,
        status: lest ? NotificationStatus.NOT_DONE : NotificationStatus.DONE,
      }));
      mutation.mutate(
        { notifikasjoner: payload },
        {
          onSuccess: async () => {
            await queryClient.invalidateQueries({
              queryKey: ["notifikasjoner"],
              type: "all",
            });

            await queryClient.invalidateQueries({
              queryKey: QueryKeys.antallUlesteNotifikasjoner(),
              type: "all",
            });

            revalidator.revalidate();
            navigate(`/arbeidsbenk/notifikasjoner${lest ? "" : "/tidligere"}`);
            return null;
          },
        },
      );
    }
  }

  if (notifikasjoner.data.length === 0) {
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
          {notifikasjoner.data.map((n) => {
            return <Notifikasjonssrad lest={lest} key={n.id} notifikasjon={n} />;
          })}
        </ul>
      </div>
    </ReloadAppErrorBoundary>
  );
}
