import { Button, Search } from "@navikt/ds-react";
import { useAtom } from "jotai";
import {
  Avtale,
  Norg2Type,
  TiltaksgjennomforingStatus,
  Tiltakstypestatus,
  VirksomhetTil,
} from "mulighetsrommet-api-client";
import { useEffect, useState } from "react";
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
import { RESET } from "jotai/vanilla/utils";
import { useNavigate } from "react-router-dom";

type Filters = "tiltakstype";

interface Props {
  skjulFilter?: Record<Filters, boolean>;
  avtale?: Avtale;
}

export function Tiltaksgjennomforingfilter({ skjulFilter, avtale }: Props) {
  const navigate = useNavigate();
  const [filter, setFilter] = useAtom(tiltaksgjennomforingfilter);
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
      ...filter,
    },
  });
  const { register } = form;

  useEffect(() => {
    // Reset filter når vi unmounter
    return () => setFilter(RESET);
  }, []);

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
            filter.fylkesenhet === ""
              ? true
              : filter.fylkesenhet === enhet.overordnetEnhet;
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
              onChange={(search: string) => setFilter({ ...filter, search })}
              value={filter.search}
              aria-label="Søk etter tiltaksgjennomføring"
              data-testid="filter_sokefelt"
              className={styles.form_field}
            />
            <SokeSelect
              size="small"
              label="Filtrer på region"
              placeholder="Filtrer på region"
              hideLabel
              {...register("fylkesenhet")}
              onChange={(fylkesenhet) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  enhet: "",
                  fylkesenhet,
                });
              }}
              options={regionOptions()}
              className={styles.form_field}
            />

            <SokeSelect
              size="small"
              label="Filtrer på enhet"
              placeholder="Filtrer på enhet"
              hideLabel
              {...register("enhet")}
              onChange={(enhet) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  enhet,
                });
              }}
              options={enhetOptions()}
              className={styles.form_field}
            />
            {skjulFilter?.tiltakstype ? null : (
              <SokeSelect
                size="small"
                label="Filtrer på tiltakstype"
                placeholder="Filtrer på tiltakstype"
                hideLabel
                {...register("tiltakstype")}
                onChange={(tiltakstype) => {
                  resetPaginering(setPage);
                  setFilter({
                    ...filter,
                    tiltakstype,
                  });
                }}
                options={tiltakstypeOptions()}
                className={styles.form_field}
              />
            )}
            <SokeSelect
              size="small"
              label="Filtrer på status"
              placeholder="Filtrer på status"
              hideLabel
              {...register("status")}
              onChange={(status) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
                  status,
                });
              }}
              options={statusOptions()}
              className={styles.form_field}
            />
            <SokeSelect
              size="small"
              label="Filtrer på arrangør"
              placeholder="Filtrer på arrangør"
              hideLabel
              {...register("arrangorOrgnr")}
              onChange={(arrangorOrgnr) => {
                resetPaginering(setPage);
                setFilter({
                  ...filter,
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
                      onSuccess={(id) => navigate(`/tiltaksgjennomforinger/${id}`)}
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
