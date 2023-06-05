import { Button, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  Norg2Type,
  TiltaksgjennomforingStatus,
  Tiltakstypestatus,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { useState } from "react";
import { FormProvider, useForm } from "react-hook-form";
import {
  Tiltaksgjennomforingfilter as TiltaksgjennomforingAtomFilter,
  paginationAtom,
  tiltaksgjennomforingfilter,
} from "../../api/atoms";
import { useAlleEnheter } from "../../api/enhet/useAlleEnheter";
import {
  OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE,
  useFeatureToggles,
} from "../../api/features/feature-toggles";
import { useTiltakstyper } from "../../api/tiltakstyper/useTiltakstyper";
import { useVirksomheter } from "../../api/virksomhet/useVirksomheter";
import { inneholderUrl, resetPaginering } from "../../utils/Utils";
import { arenaKodeErAftEllerVta } from "../../utils/tiltakskoder";
import { LeggTilGjennomforingModal } from "../modal/LeggTilGjennomforingModal";
import { OpprettTiltaksgjennomforingModal } from "../modal/OpprettTiltaksgjennomforingModal";
import { SokeSelect } from "../skjema/SokeSelect";
import styles from "./Filter.module.scss";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
  avtale?: Avtale;
}

export function Tiltaksgjennomforingfilter({ skjulFilter, avtale }: Props) {
  const [sokefilter, setSokefilter] = useAtom(tiltaksgjennomforingfilter);
  const [, setPage] = useAtom(paginationAtom);
  const { data: enheter } = useAlleEnheter();
  const { data: virksomheter } = useVirksomheter(
    VirksomhetTil.TILTAKSGJENNOMFORING
  );
  const { data: tiltakstyper } = useTiltakstyper(
    {
      status: Tiltakstypestatus.AKTIV,
    },
    1
  );
  const [opprettModal, setOpprettModalOpen] = useState<boolean>(false);
  const [modalOpen, setModalOpen] = useState<boolean>(false);

  const form = useForm<TiltaksgjennomforingAtomFilter>({
    defaultValues: {
      ...sokefilter,
    },
  });
  const { register } = form;

  const features = useFeatureToggles();
  const visOpprettTiltaksgjennomforingKnapp =
    features.isSuccess &&
    features.data[OPPRETT_TILTAKSGJENNOMFORING_ADMIN_FLATE] &&
    inneholderUrl("/avtaler/");

  const erAFTellerVTA = arenaKodeErAftEllerVta(avtale?.tiltakstype.arenaKode);

  const regionOptions = () => {
    const options =
      enheter
        ?.filter((enhet) => enhet.type === Norg2Type.FYLKE)
        ?.sort()
        ?.map((enhet) => ({ label: enhet.navn, value: enhet.enhetsnummer })) ||
      [];

    return [{ value: "", label: "Alle regioner" }, ...options];
  };

  const enhetOptions = () => {
    const options =
      enheter
        ?.filter((enhet) => {
          const erLokalEllerTiltaksenhet =
            enhet.type === Norg2Type.LOKAL || enhet.type === Norg2Type.TILTAK;
          const enheterFraFylke =
            sokefilter.fylkesenhet === ""
              ? true
              : sokefilter.fylkesenhet === enhet.overordnetEnhet;
          return erLokalEllerTiltaksenhet && enheterFraFylke;
        })
        ?.sort()
        ?.map((enhet) => ({
          label: `${enhet.navn} - ${enhet.enhetsnummer}`,
          value: enhet.enhetsnummer,
        })) || [];

    return [{ value: "", label: "Alle enheter" }, ...options];
  };

  const tiltakstypeOptions = () => {
    const options =
      tiltakstyper?.data?.sort()?.map((tiltakstype) => ({
        label: tiltakstype.navn,
        value: tiltakstype.id,
      })) || [];

    return [{ value: "", label: "Alle tiltakstyper" }, ...options];
  };

  const statusOptions = () => {
    return [
      { label: "Gjennomføres", value: TiltaksgjennomforingStatus.GJENNOMFORES },
      { label: "Avsluttet", value: TiltaksgjennomforingStatus.AVSLUTTET },
      { label: "Avbrutt", value: TiltaksgjennomforingStatus.AVBRUTT },
      {
        label: "Åpent for innsøk",
        value: TiltaksgjennomforingStatus.APENT_FOR_INNSOK,
      },
      { label: "Alle statuser", value: "" },
    ];
  };

  const arrangorOptions = () => {
    const options =
      virksomheter?.sort()?.map((virksomhet) => ({
        label: virksomhet.navn,
        value: virksomhet.organisasjonsnummer,
      })) || [];

    return [{ value: "", label: "Alle arrangører" }, ...options];
  };

  return (
    <FormProvider {...form}>
      <form>
        <div className={styles.filter_container}>
          <div className={styles.filter_left}>
            <Search
              label="Søk etter tiltaksgjennomføring"
              hideLabel
              size="small"
              variant="simple"
              onChange={(search: string) =>
                setSokefilter({ ...sokefilter, search })
              }
              value={sokefilter.search}
              aria-label="Søk etter tiltaksgjennomføring"
              data-testid="filter_sokefelt"
              className={styles.form_field}
            />
            <SokeSelect
              label="Filtrer på region"
              placeholder="Filtrer på region"
              hideLabel
              {...register("fylkesenhet")}
              onChange={(fylkesenhet) => {
                resetPaginering(setPage);
                setSokefilter({
                  ...sokefilter,
                  enhet: "",
                  fylkesenhet,
                });
              }}
              options={regionOptions()}
              className={styles.form_field}
            />

            <SokeSelect
              label="Filtrer på enhet"
              placeholder="Filtrer på enhet"
              hideLabel
              {...register("enhet")}
              onChange={(enhet) => {
                resetPaginering(setPage);
                setSokefilter({
                  ...sokefilter,
                  enhet,
                });
              }}
              options={enhetOptions()}
              className={styles.form_field}
            />
            {skjulFilter?.tiltakstype ? null : (
              <SokeSelect
                label="Filtrer på tiltakstype"
                placeholder="Filtrer på tiltakstype"
                hideLabel
                {...register("tiltakstype")}
                onChange={(tiltakstype) => {
                  resetPaginering(setPage);
                  setSokefilter({
                    ...sokefilter,
                    tiltakstype,
                  });
                }}
                options={tiltakstypeOptions()}
                className={styles.form_field}
              />
            )}
            <SokeSelect
              label="Filtrer på status"
              placeholder="Filtrer på status"
              hideLabel
              {...register("status")}
              onChange={(status) => {
                resetPaginering(setPage);
                setSokefilter({
                  ...sokefilter,
                  status,
                });
              }}
              options={statusOptions()}
              className={styles.form_field}
            />
            <SokeSelect
              label="Filtrer på arrangør"
              placeholder="Filtrer på arrangør"
              hideLabel
              {...register("arrangorOrgnr")}
              onChange={(arrangorOrgnr) => {
                resetPaginering(setPage);
                setSokefilter({
                  ...sokefilter,
                  arrangorOrgnr,
                });
              }}
              options={arrangorOptions()}
              className={styles.form_field}
            />
          </div>
          <div className={styles.filter_right}>
            {avtale && (
              <>
                {visOpprettTiltaksgjennomforingKnapp && (
                  <>
                    <Button
                      size="small"
                      onClick={() => setOpprettModalOpen(true)}
                      type="button"
                    >
                      Opprett ny gjennomføring
                    </Button>

                    <OpprettTiltaksgjennomforingModal
                      modalOpen={opprettModal}
                      avtale={avtale}
                      onClose={() => setOpprettModalOpen(false)}
                    />
                  </>
                )}
                {erAFTellerVTA && (
                  <>
                    <Button
                      size="small"
                      onClick={() => setModalOpen(true)}
                      variant="secondary"
                      type="button"
                    >
                      Legg til gjennomføring
                    </Button>
                    <LeggTilGjennomforingModal
                      modalOpen={modalOpen}
                      onClose={() => setModalOpen(false)}
                    />
                  </>
                )}
              </>
            )}
          </div>
        </div>
      </form>
    </FormProvider>
  );
}
