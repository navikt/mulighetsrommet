import { Button } from "@navikt/ds-react";
import { useAtom, WritableAtom } from "jotai";
import { Avtalestatus, Opphav, Toggles } from "mulighetsrommet-api-client";
import { useState } from "react";
import { defaultTiltaksgjennomforingfilter, TiltaksgjennomforingFilter } from "../../api/atoms";
import { useAvtale } from "../../api/avtaler/useAvtale";
import { useFeatureToggle } from "../../api/features/feature-toggles";
import { inneholderUrl } from "../../utils/Utils";
import { Lenkeknapp } from "../lenkeknapp/Lenkeknapp";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import { HarSkrivetilgang } from "../authActions/HarSkrivetilgang";

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
      filter.navEnheter.length > 0 ||
      filter.statuser.length > 0 ||
      filter.arrangorOrgnr.length > 0 ? (
        <Button
          type="button"
          size="small"
          style={{ maxWidth: "130px" }}
          variant="tertiary"
          onClick={() => {
            setFilter({
              ...defaultTiltaksgjennomforingfilter,
              avtale: avtale?.id ?? defaultTiltaksgjennomforingfilter.avtale,
            });
          }}
        >
          Nullstill filter
        </Button>
      ) : (
        <div></div>
      )}
      {/*
        Empty div over for å dytte de andre knappene til høyre uavhengig
        av om nullstill knappen er der
      */}
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
          <HarSkrivetilgang ressurs="Tiltaksgjennomføring">
            {visOpprettTiltaksgjennomforingKnapp && (
              <Lenkeknapp
                size="small"
                to={`skjema`}
                variant="primary"
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
                  avtale={avtale}
                  modalOpen={modalOpen}
                  onClose={() => setModalOpen(false)}
                />
              </>
            )}
          </HarSkrivetilgang>
        </div>
      )}
    </div>
  );
}
