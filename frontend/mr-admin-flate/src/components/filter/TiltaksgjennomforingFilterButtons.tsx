import { faro } from "@grafana/faro-web-sdk";
import { Button } from "@navikt/ds-react";
import { Avtalestatus, Opphav, Toggles } from "mulighetsrommet-api-client";
import { useState } from "react";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { inneholderUrl } from "../../utils/Utils";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import {
  defaultTiltaksgjennomforingfilter,
  TiltaksgjennomforingFilter,
} from "../../api/atoms";
import { useAtom, WritableAtom } from "jotai";

interface Props {
  filterAtom: WritableAtom<
    TiltaksgjennomforingFilter,
    [newValue: TiltaksgjennomforingFilter],
    void
  >;
}

export function TiltaksgjennomforingFilterButtons({ filterAtom }: Props) {
  const [filter, setFilter] = useAtom(filterAtom);
  const { data: avtale } = useAvtale();
  const [modalOpen, setModalOpen] = useState<boolean>(false);

  const { data: opprettGjennomforingIsEnabled } = useFeatureToggle(
    Toggles.MULIGHETSROMMET_ADMIN_FLATE_OPPRETT_TILTAKSGJENNOMFORING,
  );
  const visOpprettTiltaksgjennomforingKnapp =
    opprettGjennomforingIsEnabled && inneholderUrl("/avtaler/");

  const avtaleErOpprettetIAdminFlate = avtale?.opphav === Opphav.MR_ADMIN_FLATE;
  const avtalenErAktiv = avtale?.avtalestatus === Avtalestatus.AKTIV;

  return (
    <div
      style={{
        display: "flex",
        flexDirection: "row",
        justifyContent: "space-between",
        height: "100%",
        alignItems: "center",
      }}
    >
      {filter.search.length > 0 ||
      filter.navRegioner.length > 0 ||
      filter.tiltakstyper.length > 0 ||
      filter.statuser.length > 0 ||
      filter.arrangorOrgnr.length > 0 ? (
        <Button
          type="button"
          size="small"
          style={{ maxWidth: "130px" }}
          variant="tertiary"
          onClick={() => {
            setFilter({ ...defaultTiltaksgjennomforingfilter });
          }}
        >
          Nullstill filter
        </Button>
      ) : (
        <div></div>
      )}
      {avtale && avtalenErAktiv && (
        <div
          style={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "end",
            gap: "1rem",
            alignItems: "center",
          }}
        >
          {visOpprettTiltaksgjennomforingKnapp && (
            <Lenkeknapp
              size="small"
              to={`skjema`}
              variant="primary"
              handleClick={() => {
                faro?.api?.pushEvent("Bruker trykket på 'Opprett ny tiltaksgjennomføring'-knapp");
              }}
              dataTestid="opprett-ny-tiltaksgjenomforing_knapp"
            >
              Opprett ny tiltaksgjennomføring
            </Lenkeknapp>
          )}
          {avtaleErOpprettetIAdminFlate && (
            <>
              <Button
                size="small"
                onClick={() => setModalOpen(true)}
                variant="secondary"
                type="button"
                title="Legg til en eksisterende gjennomføring til avtalen"
              >
                Legg til gjennomføring
              </Button>
              <LeggTilGjennomforingModal
                modalOpen={modalOpen}
                onClose={() => setModalOpen(false)}
              />
            </>
          )}
        </div>
      )}
    </div>
  );
}
